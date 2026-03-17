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
        Optional<AlarmEntity> cachedAlarm = realtimeStateService.getActiveAlarmId(result.getAlarmType(), resolved.getPartitionCode())
            .flatMap(alarmRepository::findById);
        AlarmEntity alarm = cachedAlarm.orElseGet(() ->
            alarmRepository.findOpenAlarm(resolved.getMonitor().getId(), resolved.getPartitionCode(), result.getAlarmType()).orElse(null)
        );

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
            alarm.setStatus("ACTIVE");
            alarm.setFirstAlarmTime(eventTime);
            alarm.setMergeCount(1);
            alarm.setAlarmLevel(result.getAlarmLevel());
            alarm.setTitle(result.getTitle());
            alarm.setContent(result.getContent());
            alarm.setDeleted(0);
            alarm.setCreatedOn(eventTime);
        } else {
            alarm.setMergeCount(alarm.getMergeCount() == null ? 1 : alarm.getMergeCount() + 1);
            alarm.setContent(result.getContent());
        }
        alarm.setLastAlarmTime(eventTime);
        alarm.setUpdatedOn(eventTime);
        AlarmEntity savedAlarm = alarmRepository.save(alarm);
        realtimeStateService.setActiveAlarmId(result.getAlarmType(), resolved.getPartitionCode(), savedAlarm.getId());

        if (!merged || realtimeStateService.shouldWriteMergedEvent(result.getAlarmType(), resolved.getPartitionCode(), eventTime)) {
            createEvent(
                savedAlarm,
                resolved,
                result.getAlarmType(),
                result.getSourceType(),
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
        AlarmEntity alarm = realtimeStateService.getActiveAlarmId(alarmType, resolved.getPartitionCode())
            .flatMap(alarmRepository::findById)
            .orElseGet(() -> alarmRepository.findOpenAlarm(resolved.getMonitor().getId(), resolved.getPartitionCode(), alarmType).orElse(null));
        if (alarm == null) {
            return;
        }
        alarm.setStatus("RECOVERED");
        alarm.setLastAlarmTime(eventTime);
        alarm.setUpdatedOn(eventTime);
        alarmRepository.save(alarm);
        realtimeStateService.clearActiveAlarmId(alarmType, resolved.getPartitionCode());

        createEvent(
            alarm,
            resolved,
            alarmType,
            "RECOVERY",
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
        alarm.setStatus("CONFIRMED");
        alarm.setConfirmUserId(userId);
        alarm.setConfirmTime(LocalDateTime.now());
        alarm.setHandleRemark(remark);
        alarm.setUpdatedOn(LocalDateTime.now());
        AlarmEntity savedAlarm = alarmRepository.save(alarm);
        createLifecycleEvent(savedAlarm, "MANUAL_CONFIRM", remark);
        return savedAlarm;
    }

    @Override
    @Transactional
    public AlarmEntity close(Long alarmId, String remark) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        alarm.setStatus("CLOSED");
        alarm.setHandleRemark(remark);
        alarm.setUpdatedOn(LocalDateTime.now());
        AlarmEntity savedAlarm = alarmRepository.save(alarm);
        realtimeStateService.clearActiveAlarmId(savedAlarm.getAlarmType(), savedAlarm.getPartitionCode());
        createLifecycleEvent(savedAlarm, "MANUAL_CLOSE", remark);
        return savedAlarm;
    }

    private void createLifecycleEvent(AlarmEntity alarm, String sourceType, String remark) {
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
        event.setEventTime(eventTime);
        event.setEventNo(eventNo);
        event.setEventLevel(eventLevel);
        event.setPointListJson(pointListJson);
        event.setDetailJson(detailJson);
        event.setMergedFlag(mergedFlag);
        event.setDeleted(0);
        event.setCreatedOn(eventTime);
        event.setUpdatedOn(eventTime);
        eventJdbcRepository.insert(event);
        realtimeStateService.markEventWritten(alarmType, resolved.getPartitionCode(), eventTime);
    }
}
