package com.example.demo.alarm;

import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.persistence.entity.AlarmEntity;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.EventEntity;
import com.example.demo.persistence.repository.AlarmJdbcRepository;
import com.example.demo.persistence.repository.AlarmRepository;
import com.example.demo.persistence.repository.EventRepository;
import com.example.demo.persistence.repository.EventJdbcRepository;
import com.example.demo.realtime.RealtimeStateService;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.support.IdGenerator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmMergeService implements AlarmService {

    private static final DateTimeFormatter CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AlarmJdbcRepository alarmJdbcRepository;
    private final AlarmRepository alarmRepository;
    private final EventRepository eventRepository;
    private final EventJdbcRepository eventJdbcRepository;
    private final RealtimeStateService realtimeStateService;
    private final IdGenerator idGenerator;

    public AlarmMergeService(
        AlarmJdbcRepository alarmJdbcRepository,
        AlarmRepository alarmRepository,
        EventRepository eventRepository,
        EventJdbcRepository eventJdbcRepository,
        RealtimeStateService realtimeStateService,
        IdGenerator idGenerator
    ) {
        this.alarmJdbcRepository = alarmJdbcRepository;
        this.alarmRepository = alarmRepository;
        this.eventRepository = eventRepository;
        this.eventJdbcRepository = eventJdbcRepository;
        this.realtimeStateService = realtimeStateService;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public AlarmEntity createOrMerge(
        DeviceResolverService.ResolvedTarget resolved,
        RuleEvaluationResult result,
        LocalDateTime eventTime,
        String detailJson,
        String pointListJson
    ) {
        String mergeKey = buildMergeKey(resolved.getMonitor().getId(), result.getAlarmType());
        AlarmEntity candidate = new AlarmEntity();
        candidate.setId(idGenerator.nextId());
        candidate.setAlarmCode(result.getAlarmType() + "-" + CODE_FORMATTER.format(eventTime) + "-" + candidate.getId());
        candidate.setAlarmType(result.getAlarmType());
        candidate.setStatus(AlarmStatus.PENDING_CONFIRM);
        candidate.setFirstAlarmTime(eventTime);
        candidate.setMergeCount(1);
        candidate.setEventCount(0);
        candidate.setDeleted(0);
        candidate.setCreatedOn(eventTime);
        candidate.setSourceType(result.getSourceType());
        candidate.setMonitorId(resolved.getMonitor().getId());
        candidate.setDeviceId(resolved.getDevice().getId());
        candidate.setShaftFloorId(resolved.getShaftFloorId());
        candidate.setPartitionCode(resolved.getPartitionCode());
        candidate.setPartitionName(resolved.getPartitionName());
        candidate.setDataReference(resolved.getDataReference());
        candidate.setDeviceToken(resolved.getDeviceToken());
        candidate.setPartitionNo(resolved.getPartitionNo());
        candidate.setSourceFormat(resolved.getSourceFormat());
        candidate.setMergeKey(mergeKey);
        candidate.setAlarmLevel(result.getAlarmLevel());
        candidate.setTitle(result.getTitle());
        candidate.setContent(result.getContent());
        candidate.setLastAlarmTime(eventTime);
        candidate.setUpdatedOn(eventTime);

        boolean inserted = alarmJdbcRepository.upsertPendingAlarm(candidate);
        AlarmEntity savedAlarm = alarmRepository.findByMergeKey(mergeKey)
            .orElseThrow(() -> new IllegalStateException("Alarm not found after upsert: " + mergeKey));
        boolean merged = !inserted;
        realtimeStateService.setActiveAlarmId(result.getAlarmType(), String.valueOf(resolved.getMonitor().getId()), savedAlarm.getId());

        if (!merged || realtimeStateService.shouldWriteMergedEvent(result.getAlarmType(), String.valueOf(resolved.getMonitor().getId()), eventTime)) {
            createEvent(
                savedAlarm,
                resolved,
                result.getAlarmType(),
                result.getSourceType(),
                merged ? AlarmEventType.MERGE : AlarmEventType.TRIGGER,
                result.getContent(),
                eventTime,
                savedAlarm.getMergeCount(),
                result.getAlarmLevel(),
                pointListJson,
                detailJson,
                merged ? 1 : 0
            );
        }
        return savedAlarm;
    }

    @Override
    @Transactional
    public void recover(
        DeviceResolverService.ResolvedTarget resolved,
        String alarmType,
        LocalDateTime eventTime,
        String detailJson
    ) {
        AlarmEntity alarm = realtimeStateService.getActiveAlarmId(alarmType, String.valueOf(resolved.getMonitor().getId()))
            .flatMap(alarmRepository::findById)
            .orElseGet(() -> alarmRepository.findByMergeKey(buildMergeKey(resolved.getMonitor().getId(), alarmType)).orElse(null));
        if (alarm == null) {
            return;
        }
        alarm.setMergeKey(null);
        alarm.setStatus(AlarmStatus.AUTO_RECOVERED);
        alarm.setLastAlarmTime(eventTime);
        alarm.setUpdatedOn(eventTime);
        alarmRepository.save(alarm);
        realtimeStateService.clearActiveAlarmId(alarmType, String.valueOf(resolved.getMonitor().getId()));

        createEvent(
            alarm,
            resolved,
            alarmType,
            "RECOVERY",
            AlarmEventType.RECOVER,
            "告警已恢复",
            eventTime,
            (alarm.getMergeCount() == null ? 0 : alarm.getMergeCount()) + 1,
            alarm.getAlarmLevel(),
            "[]",
            detailJson,
            0
        );
    }

    @Override
    @Transactional
    public AlarmEntity confirm(Long alarmId, Long userId, String remark) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        alarm.setMergeKey(null);
        alarm.setStatus(AlarmStatus.CONFIRMED);
        alarm.setConfirmUserId(userId);
        alarm.setConfirmTime(LocalDateTime.now());
        alarm.setHandleRemark(remark);
        alarm.setUpdatedOn(LocalDateTime.now());
        AlarmEntity savedAlarm = alarmRepository.save(alarm);
        realtimeStateService.clearActiveAlarmId(savedAlarm.getAlarmType(), String.valueOf(savedAlarm.getMonitorId()));
        createLifecycleEvent(savedAlarm, "MANUAL_CONFIRM", AlarmEventType.CONFIRM, remark);
        return savedAlarm;
    }

    @Override
    @Transactional
    public AlarmEntity observe(Long alarmId, String remark) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        alarm.setMergeKey(null);
        alarm.setStatus(AlarmStatus.OBSERVING);
        alarm.setHandleRemark(remark);
        alarm.setUpdatedOn(LocalDateTime.now());
        AlarmEntity savedAlarm = alarmRepository.save(alarm);
        realtimeStateService.clearActiveAlarmId(savedAlarm.getAlarmType(), String.valueOf(savedAlarm.getMonitorId()));
        createLifecycleEvent(savedAlarm, "MANUAL_OBSERVE", AlarmEventType.OBSERVE, remark);
        return savedAlarm;
    }

    @Override
    @Transactional
    public AlarmEntity markFalsePositive(Long alarmId, String remark) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        alarm.setMergeKey(null);
        alarm.setStatus(AlarmStatus.FALSE_POSITIVE);
        alarm.setHandleRemark(remark);
        alarm.setUpdatedOn(LocalDateTime.now());
        AlarmEntity savedAlarm = alarmRepository.save(alarm);
        realtimeStateService.clearActiveAlarmId(savedAlarm.getAlarmType(), String.valueOf(savedAlarm.getMonitorId()));
        createLifecycleEvent(savedAlarm, "MANUAL_FALSE", AlarmEventType.FALSE_POSITIVE, remark);
        return savedAlarm;
    }

    @Override
    @Transactional
    public AlarmEntity close(Long alarmId, String remark) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        alarm.setMergeKey(null);
        alarm.setStatus(AlarmStatus.CLOSED);
        alarm.setHandleRemark(remark);
        alarm.setUpdatedOn(LocalDateTime.now());
        AlarmEntity savedAlarm = alarmRepository.save(alarm);
        realtimeStateService.clearActiveAlarmId(savedAlarm.getAlarmType(), String.valueOf(savedAlarm.getMonitorId()));
        createLifecycleEvent(savedAlarm, "MANUAL_CLOSE", AlarmEventType.CLOSE, remark);
        return savedAlarm;
    }

    private void createLifecycleEvent(AlarmEntity alarm, String sourceType, Integer eventType, String remark) {
        DeviceEntity device = new DeviceEntity();
        device.setId(alarm.getDeviceId());
        com.example.demo.persistence.entity.MonitorEntity monitor = new com.example.demo.persistence.entity.MonitorEntity();
        monitor.setId(alarm.getMonitorId());
        DeviceResolverService.ResolvedTarget resolved = new DeviceResolverService.ResolvedTarget(
            device,
            monitor,
            null,
            alarm.getPartitionCode(),
            alarm.getPartitionName(),
            alarm.getDataReference(),
            alarm.getDeviceToken(),
            alarm.getPartitionNo(),
            alarm.getSourceFormat()
        );
        createEvent(
            alarm,
            resolved,
            alarm.getAlarmType(),
            sourceType,
            eventType,
            remark == null || remark.trim().isEmpty() ? alarm.getContent() : remark,
            LocalDateTime.now(),
            (alarm.getMergeCount() == null ? 0 : alarm.getMergeCount()) + 1,
            alarm.getAlarmLevel(),
            "[]",
            "{\"remark\":\"" + (remark == null ? "" : remark.replace("\"", "\\\"")) + "\"}",
            0
        );
    }

    private void createEvent(
        AlarmEntity alarm,
        DeviceResolverService.ResolvedTarget resolved,
        String alarmType,
        String sourceType,
        Integer eventType,
        String content,
        LocalDateTime eventTime,
        Integer eventNo,
        Integer eventLevel,
        String pointListJson,
        String detailJson,
        Integer mergedFlag
    ) {
        EventEntity event = new EventEntity();
        event.setId(idGenerator.nextId());
        event.setAlarmId(alarm.getId());
        event.setAlarmType(alarmType);
        event.setSourceType(sourceType);
        event.setMonitorId(resolved.getMonitor().getId());
        event.setDeviceId(resolved.getDevice().getId());
        event.setShaftFloorId(resolved.getShaftFloorId());
        event.setPartitionCode(resolved.getPartitionCode());
        event.setPartitionName(resolved.getPartitionName());
        event.setDataReference(resolved.getDataReference());
        event.setDeviceToken(resolved.getDeviceToken());
        event.setPartitionNo(resolved.getPartitionNo());
        event.setSourceFormat(resolved.getSourceFormat());
        event.setEventType(eventType);
        event.setEventTime(eventTime);
        event.setEventNo(eventNo);
        event.setEventLevel(eventLevel);
        event.setPointListJson(pointListJson);
        event.setDetailJson(detailJson);
        event.setContent(content);
        event.setMergedFlag(mergedFlag);
        event.setDeleted(0);
        event.setCreatedOn(eventTime);
        event.setUpdatedOn(eventTime);
        eventJdbcRepository.insert(event);
        alarm.setEventCount(alarm.getEventCount() == null ? 1 : alarm.getEventCount() + 1);
        alarmRepository.save(alarm);
        realtimeStateService.markEventWritten(alarmType, String.valueOf(resolved.getMonitor().getId()), eventTime);
    }

    private String buildMergeKey(Long monitorId, String alarmType) {
        return monitorId + ":" + alarmType;
    }
}
