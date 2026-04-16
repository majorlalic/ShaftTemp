package com.example.demo.service;

import com.example.demo.service.RuleEvaluationResult;
import com.example.demo.entity.AlarmEntity;
import com.example.demo.entity.DeviceEntity;
import com.example.demo.entity.EventEntity;
import com.example.demo.dao.AlarmRepository;
import com.example.demo.dao.EventRepository;
import com.example.demo.service.RealtimeStateService;
import com.example.demo.service.DeviceResolverService;
import com.example.demo.service.IdGenerator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmMergeService implements AlarmService {

    private static final DateTimeFormatter CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AlarmRepository alarmRepository;
    private final EventRepository eventRepository;
    private final RealtimeStateService realtimeStateService;
    private final IdGenerator idGenerator;

    public AlarmMergeService(
        AlarmRepository alarmRepository,
        EventRepository eventRepository,
        RealtimeStateService realtimeStateService,
        IdGenerator idGenerator
    ) {
        this.alarmRepository = alarmRepository;
        this.eventRepository = eventRepository;
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
        candidate.setId(String.valueOf(idGenerator.nextId()));
        candidate.setAlarmCode(result.getAlarmType() + "-" + CODE_FORMATTER.format(eventTime) + "-" + candidate.getId());
        candidate.setAlarmType(result.getAlarmType());
        candidate.setAlarmTypeBig(Integer.valueOf(AlarmTypeBig.SHAFT_TEMP));
        candidate.setAlarmDomain(AlarmDomain.fromAlarmType(result.getAlarmType()));
        candidate.setStatus(AlarmStatus.PENDING_CONFIRM);
        candidate.setFirstAlarmTime(eventTime);
        candidate.setMergeCount(1);
        candidate.setEventCount(0);
        candidate.setDeleted(0);
        candidate.setCreatedOn(eventTime);
        candidate.setSourceType(result.getSourceType());
        candidate.setMonitorId(String.valueOf(resolved.getMonitor().getId()));
        candidate.setDeviceId(String.valueOf(resolved.getDevice().getId()));
        candidate.setShaftFloorId(resolved.getShaftFloorId());
        candidate.setPartitionCode(resolved.getPartitionCode());
        candidate.setPartitionName(resolved.getPartitionName());
        candidate.setDataReference(resolved.getDataReference());
        candidate.setDeviceToken(resolved.getDeviceToken());
        candidate.setPartitionNo(resolved.getPartitionNo());
        candidate.setSourceFormat(resolved.getSourceFormat());
        candidate.setAreaName(resolved.getMonitor().getAreaName());
        candidate.setMonitorName(resolved.getMonitor().getName());
        candidate.setDeviceName(resolved.getDevice().getName());
        candidate.setManufacturer(resolved.getDevice().getManufacturer());
        candidate.setDeviceModel(resolved.getDevice().getModel());
        candidate.setMergeKey(mergeKey);
        candidate.setAlarmLevel(result.getAlarmLevel());
        candidate.setTitle(result.getTitle());
        candidate.setContent(result.getContent());
        candidate.setPushStatus(0);
        candidate.setPushTime(null);
        candidate.setLastAlarmTime(eventTime);
        candidate.setUpdatedOn(eventTime);

        AlarmEntity savedAlarm;
        boolean merged;
        try {
            alarmRepository.upsertPendingAlarm(candidate);
            savedAlarm = alarmRepository.findById(parseLongId(candidate.getId()))
                .orElseThrow(() -> new IllegalStateException("Alarm not found after insert: " + candidate.getId()));
            merged = false;
        } catch (RuntimeException ex) {
            if (!isPendingMergeConflict(ex)) {
                throw ex;
            }
            savedAlarm = alarmRepository.findByMergeKey(mergeKey)
                .orElseThrow(() -> new IllegalStateException("Alarm not found after duplicate key: " + mergeKey));
            savedAlarm.setSourceType(result.getSourceType());
            savedAlarm.setAlarmDomain(AlarmDomain.fromAlarmType(result.getAlarmType()));
            savedAlarm.setDeviceId(String.valueOf(resolved.getDevice().getId()));
            savedAlarm.setShaftFloorId(resolved.getShaftFloorId());
            savedAlarm.setPartitionCode(resolved.getPartitionCode());
            savedAlarm.setPartitionName(resolved.getPartitionName());
            savedAlarm.setDataReference(resolved.getDataReference());
            savedAlarm.setDeviceToken(resolved.getDeviceToken());
            savedAlarm.setPartitionNo(resolved.getPartitionNo());
            savedAlarm.setSourceFormat(resolved.getSourceFormat());
            savedAlarm.setAreaName(resolved.getMonitor().getAreaName());
            savedAlarm.setMonitorName(resolved.getMonitor().getName());
            savedAlarm.setDeviceName(resolved.getDevice().getName());
            savedAlarm.setManufacturer(resolved.getDevice().getManufacturer());
            savedAlarm.setDeviceModel(resolved.getDevice().getModel());
            savedAlarm.setLastAlarmTime(eventTime);
            savedAlarm.setMergeCount(savedAlarm.getMergeCount() == null ? 1 : savedAlarm.getMergeCount() + 1);
            savedAlarm.setAlarmLevel(result.getAlarmLevel());
            savedAlarm.setTitle(result.getTitle());
            savedAlarm.setContent(result.getContent());
            savedAlarm.setUpdatedOn(eventTime);
            alarmRepository.updateById(savedAlarm);
            merged = true;
        }
        realtimeStateService.setActiveAlarmId(
            result.getAlarmType(),
            String.valueOf(resolved.getMonitor().getId()),
            parseLongId(savedAlarm.getId())
        );

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

    private boolean isPendingMergeConflict(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof DuplicateKeyException) {
                // upsertPendingAlarm 的唯一冲突只来自 merge_key，直接按合并路径处理
                return true;
            }
            if (cursor instanceof DataIntegrityViolationException || cursor instanceof PersistenceException) {
                String message = cursor.getMessage();
                if (message != null) {
                    String normalized = message.toLowerCase();
                    boolean hasMergeKey = normalized.contains("merge_key") || normalized.contains("uk_alarm_merge_key");
                    boolean isUniqueConflict = normalized.contains("unique")
                        || normalized.contains("duplicate")
                        || normalized.contains("唯一")
                        || normalized.contains("约束");
                    if (hasMergeKey && isUniqueConflict) {
                        return true;
                    }
                }
            }
            cursor = cursor.getCause();
        }
        return false;
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
        alarmRepository.updateById(alarm);
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
    public AlarmEntity handle(Long alarmId, Integer status, String remark) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (!isSupportedStatus(status.intValue())) {
            throw new IllegalArgumentException("Unsupported status: " + status);
        }
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        LocalDateTime now = LocalDateTime.now();
        alarm.setMergeKey(null);
        alarm.setStatus(status);
        alarm.setHandleTime(now);
        alarm.setHandleRemark(remark);
        alarm.setUpdatedOn(now);
        alarmRepository.updateById(alarm);
        AlarmEntity savedAlarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        realtimeStateService.clearActiveAlarmId(savedAlarm.getAlarmType(), String.valueOf(savedAlarm.getMonitorId()));
        createLifecycleEvent(savedAlarm, sourceTypeByStatus(status), eventTypeByStatus(status), remark);
        return savedAlarm;
    }

    @Override
    @Transactional
    public AlarmEntity confirm(Long alarmId, Long handler, String remark) {
        AlarmEntity savedAlarm = handle(alarmId, Integer.valueOf(AlarmStatus.CONFIRMED), remark);
        if (handler != null) {
            savedAlarm.setHandler(String.valueOf(handler));
            savedAlarm.setHandlerName(String.valueOf(handler));
            savedAlarm.setUpdatedOn(LocalDateTime.now());
            alarmRepository.updateById(savedAlarm);
        }
        return savedAlarm;
    }

    @Override
    @Transactional
    public AlarmEntity observe(Long alarmId, String remark) {
        return handle(alarmId, Integer.valueOf(AlarmStatus.OBSERVING), remark);
    }

    @Override
    @Transactional
    public AlarmEntity markFalsePositive(Long alarmId, String remark) {
        return handle(alarmId, Integer.valueOf(AlarmStatus.FALSE_POSITIVE), remark);
    }

    @Override
    @Transactional
    public AlarmEntity close(Long alarmId, String remark) {
        return handle(alarmId, Integer.valueOf(AlarmStatus.CLOSED), remark);
    }

    private void createLifecycleEvent(AlarmEntity alarm, String sourceType, Integer eventType, String remark) {
        DeviceEntity device = new DeviceEntity();
        device.setId(parseLongId(alarm.getDeviceId()));
        com.example.demo.entity.MonitorEntity monitor = new com.example.demo.entity.MonitorEntity();
        monitor.setId(parseLongId(alarm.getMonitorId()));
        DeviceResolverService.ResolvedTarget resolved = new DeviceResolverService.ResolvedTarget(
            device,
            monitor,
            null,
            alarm.getPartitionCode(),
            alarm.getPartitionName(),
            alarm.getDataReference(),
            null,
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
        event.setAlarmId(parseLongId(alarm.getId()));
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
        eventRepository.insert(event);
        alarm.setEventCount(alarm.getEventCount() == null ? 1 : alarm.getEventCount() + 1);
        alarmRepository.updateById(alarm);
        realtimeStateService.markEventWritten(alarmType, String.valueOf(resolved.getMonitor().getId()), eventTime);
    }

    private String buildMergeKey(Long monitorId, String alarmType) {
        return monitorId + ":" + alarmType;
    }

    private boolean isSupportedStatus(int status) {
        return status == AlarmStatus.PENDING_CONFIRM
            || status == AlarmStatus.OBSERVING
            || status == AlarmStatus.PENDING_RECTIFICATION
            || status == AlarmStatus.PENDING_RETEST
            || status == AlarmStatus.CONFIRMED
            || status == AlarmStatus.CLOSED;
    }

    private String sourceTypeByStatus(Integer status) {
        switch (status.intValue()) {
            case AlarmStatus.PENDING_CONFIRM:
                return "MANUAL_PENDING_CONFIRM";
            case AlarmStatus.OBSERVING:
                return "MANUAL_OBSERVE";
            case AlarmStatus.PENDING_RECTIFICATION:
                return "MANUAL_PENDING_RECTIFICATION";
            case AlarmStatus.PENDING_RETEST:
                return "MANUAL_PENDING_RETEST";
            case AlarmStatus.CONFIRMED:
                return "MANUAL_CONFIRM";
            case AlarmStatus.CLOSED:
                return "MANUAL_CLOSE";
            default:
                return "MANUAL_HANDLE";
        }
    }

    private Integer eventTypeByStatus(Integer status) {
        switch (status.intValue()) {
            case AlarmStatus.CONFIRMED:
                return Integer.valueOf(AlarmEventType.CONFIRM);
            case AlarmStatus.CLOSED:
                return Integer.valueOf(AlarmEventType.CLOSE);
            case AlarmStatus.PENDING_RETEST:
                return Integer.valueOf(AlarmEventType.FALSE_POSITIVE);
            default:
                return Integer.valueOf(AlarmEventType.OBSERVE);
        }
    }

    private Long parseLongId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
