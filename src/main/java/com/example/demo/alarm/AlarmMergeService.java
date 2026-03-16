package com.example.demo.alarm;

import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.persistence.entity.AlarmEntity;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.EventEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.repository.AlarmRepository;
import com.example.demo.persistence.repository.EventRepository;
import com.example.demo.realtime.RealtimeStateService;
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
        DeviceEntity device,
        MonitorEntity monitor,
        RuleEvaluationResult result,
        LocalDateTime eventTime,
        String detailJson,
        String pointListJson
    ) {
        Optional<AlarmEntity> cachedAlarm = realtimeStateService.getActiveAlarmId(result.getAlarmType(), monitor.getId())
            .flatMap(alarmRepository::findById);
        AlarmEntity alarm = cachedAlarm.orElseGet(() ->
            alarmRepository.findActiveAlarm(monitor.getId(), result.getAlarmType()).orElse(null)
        );

        boolean merged = alarm != null;
        if (!merged) {
            alarm = new AlarmEntity();
            alarm.setId(idGenerator.nextId());
            alarm.setAlarmCode(result.getAlarmType() + "-" + CODE_FORMATTER.format(eventTime) + "-" + alarm.getId());
            alarm.setAlarmType(result.getAlarmType());
            alarm.setSourceType(result.getSourceType());
            alarm.setMonitorId(monitor.getId());
            alarm.setDeviceId(device.getId());
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
        realtimeStateService.setActiveAlarmId(result.getAlarmType(), monitor.getId(), savedAlarm.getId());

        EventEntity event = new EventEntity();
        event.setId(idGenerator.nextId());
        event.setAlarmId(savedAlarm.getId());
        event.setAlarmType(result.getAlarmType());
        event.setSourceType(result.getSourceType());
        event.setMonitorId(monitor.getId());
        event.setDeviceId(device.getId());
        event.setEventTime(eventTime);
        event.setEventNo(savedAlarm.getMergeCount());
        event.setEventLevel(result.getAlarmLevel());
        event.setPointListJson(pointListJson);
        event.setDetailJson(detailJson);
        event.setMergedFlag(merged ? 1 : 0);
        event.setDeleted(0);
        event.setCreatedOn(eventTime);
        event.setUpdatedOn(eventTime);
        eventRepository.save(event);
        return savedAlarm;
    }
}
