package com.example.demo.alarm;

import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.persistence.entity.AlarmEntity;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.EventEntity;
import com.example.demo.persistence.repository.AlarmRepository;
import com.example.demo.persistence.repository.EventRepository;
import com.example.demo.persistence.repository.EventJdbcRepository;
import com.example.demo.realtime.RealtimeStateService;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.support.IdGenerator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmMergeService implements AlarmService {

    private static final DateTimeFormatter CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AlarmRepository alarmRepository;
    private final EventRepository eventRepository;
    private final EventJdbcRepository eventJdbcRepository;
    private final RealtimeStateService realtimeStateService;
    private final IdGenerator idGenerator;

    public AlarmMergeService(
        AlarmRepository alarmRepository,
        EventRepository eventRepository,
        EventJdbcRepository eventJdbcRepository,
        RealtimeStateService realtimeStateService,
        IdGenerator idGenerator
    ) {
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
        Optional<AlarmEntity> cachedAlarm = realtimeStateService
            .getActiveAlarmId(result.getAlarmType(), String.valueOf(resolved.getMonitor().getId()))
            .flatMap(alarmRepository::findById);
        AlarmEntity alarm = cachedAlarm
            .filter(this::isPendingMergeable)
            .filter(candidate -> mergeKey.equals(candidate.getMergeKey()))
            .orElseGet(() -> alarmRepository.findByMergeKey(mergeKey).orElse(null));

        boolean merged = alarm != null;
        if (!merged) {
            alarm = new AlarmEntity();
            alarm.setId(idGenerator.nextId());
            alarm.setAlarmCode(result.getAlarmType() + "-" + CODE_FORMATTER.format(eventTime) + "-" + alarm.getId());
            alarm.setAlarmType(result.getAlarmType());
            alarm.setSourceType(result.getSourceType());
            alarm.setMonitorId(resolved.getMonitor().getId());
            alarm.setDeviceId(resolved.getDevice().getId());
            alarm.setShaftFloorId(resolved.getShaftFloorId());
            alarm.setPartitionCode(resolved.getPartitionCode());
            alarm.setPartitionName(resolved.getPartitionName());
            alarm.setDataReference(resolved.getDataReference());
            alarm.setDeviceToken(resolved.getDeviceToken());
            alarm.setPartitionNo(resolved.getPartitionNo());
            alarm.setSourceFormat(resolved.getSourceFormat());
            alarm.setMergeKey(mergeKey);
            alarm.setStatus(AlarmStatus.PENDING_CONFIRM);
            alarm.setFirstAlarmTime(eventTime);
            alarm.setMergeCount(1);
            alarm.setEventCount(0);
            alarm.setAlarmLevel(result.getAlarmLevel());
            alarm.setTitle(result.getTitle());
            alarm.setContent(result.getContent());
            alarm.setDeleted(0);
            alarm.setCreatedOn(eventTime);
        } else {
            alarm.setMergeCount(alarm.getMergeCount() == null ? 1 : alarm.getMergeCount() + 1);
            alarm.setContent(result.getContent());
            alarm.setShaftFloorId(resolved.getShaftFloorId());
            alarm.setPartitionCode(resolved.getPartitionCode());
            alarm.setPartitionName(resolved.getPartitionName());
            alarm.setDataReference(resolved.getDataReference());
            alarm.setDeviceToken(resolved.getDeviceToken());
            alarm.setPartitionNo(resolved.getPartitionNo());
            alarm.setMergeKey(mergeKey);
        }
        alarm.setLastAlarmTime(eventTime);
        alarm.setUpdatedOn(eventTime);
        AlarmEntity savedAlarm;
        try {
            savedAlarm = alarmRepository.saveAndFlush(alarm);
        } catch (DataIntegrityViolationException ex) {
            if (merged) {
                throw ex;
            }
            AlarmEntity existing = alarmRepository.findByMergeKey(mergeKey).orElseThrow(() -> ex);
            existing.setMergeCount(existing.getMergeCount() == null ? 1 : existing.getMergeCount() + 1);
            existing.setContent(result.getContent());
            existing.setShaftFloorId(resolved.getShaftFloorId());
            existing.setPartitionCode(resolved.getPartitionCode());
            existing.setPartitionName(resolved.getPartitionName());
            existing.setDataReference(resolved.getDataReference());
            existing.setDeviceToken(resolved.getDeviceToken());
            existing.setPartitionNo(resolved.getPartitionNo());
            existing.setLastAlarmTime(eventTime);
            existing.setUpdatedOn(eventTime);
            savedAlarm = alarmRepository.saveAndFlush(existing);
            merged = true;
        }
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
        alarmRepository.saveAndFlush(alarm);
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
        AlarmEntity savedAlarm = alarmRepository.saveAndFlush(alarm);
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
        AlarmEntity savedAlarm = alarmRepository.saveAndFlush(alarm);
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
        AlarmEntity savedAlarm = alarmRepository.saveAndFlush(alarm);
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
        AlarmEntity savedAlarm = alarmRepository.saveAndFlush(alarm);
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
        alarmRepository.saveAndFlush(alarm);
        realtimeStateService.markEventWritten(alarmType, String.valueOf(resolved.getMonitor().getId()), eventTime);
    }

    private boolean isPendingMergeable(AlarmEntity alarm) {
        return alarm != null
            && alarm.getStatus() != null
            && alarm.getStatus().intValue() == AlarmStatus.PENDING_CONFIRM;
    }

    private String buildMergeKey(Long monitorId, String alarmType) {
        return monitorId + ":" + alarmType;
    }
}
