package com.example.demo.inspection;

import com.example.demo.AppProperties;
import com.example.demo.alarm.AlarmService;
import com.example.demo.alarm.rule.AlarmRuleEngine;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.DeviceOnlineLogEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.repository.DeviceOnlineLogRepository;
import com.example.demo.persistence.repository.DeviceRepository;
import com.example.demo.persistence.repository.MonitorRepository;
import com.example.demo.realtime.RealtimeStateService;
import com.example.demo.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OfflineInspectionTask {

    private final AppProperties appProperties;
    private final DeviceRepository deviceRepository;
    private final MonitorRepository monitorRepository;
    private final DeviceOnlineLogRepository deviceOnlineLogRepository;
    private final RealtimeStateService realtimeStateService;
    private final AlarmRuleEngine alarmRuleEngine;
    private final AlarmService alarmService;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public OfflineInspectionTask(
        AppProperties appProperties,
        DeviceRepository deviceRepository,
        MonitorRepository monitorRepository,
        DeviceOnlineLogRepository deviceOnlineLogRepository,
        RealtimeStateService realtimeStateService,
        AlarmRuleEngine alarmRuleEngine,
        AlarmService alarmService,
        IdGenerator idGenerator,
        ObjectMapper objectMapper
    ) {
        this.appProperties = appProperties;
        this.deviceRepository = deviceRepository;
        this.monitorRepository = monitorRepository;
        this.deviceOnlineLogRepository = deviceOnlineLogRepository;
        this.realtimeStateService = realtimeStateService;
        this.alarmRuleEngine = alarmRuleEngine;
        this.alarmService = alarmService;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${shaft.inspection.fixed-delay-ms:30000}")
    @Transactional
    public void inspect() {
        if (!appProperties.getInspection().isEnabled()) {
            return;
        }
        List<DeviceEntity> devices = deviceRepository.findAllActive();
        for (DeviceEntity device : devices) {
            Optional<LocalDateTime> reportTime = realtimeStateService.getLastReportTime(device.getId());
            LocalDateTime lastReportTime = reportTime.orElse(device.getLastReportTime());
            if (lastReportTime == null) {
                continue;
            }
            long offlineSeconds = Duration.between(lastReportTime, LocalDateTime.now()).getSeconds();
            if (offlineSeconds < appProperties.getAlarm().getOfflineThresholdSeconds()) {
                continue;
            }
            int level = realtimeStateService.incrementOfflineLevel(device.getId());
            if (level > 1 && device.getOnlineStatus() != null && device.getOnlineStatus() == 0) {
                continue;
            }
            triggerOffline(device, offlineSeconds);
        }
    }

    private void triggerOffline(DeviceEntity device, long offlineSeconds) {
        MonitorEntity monitor = monitorRepository.findActiveByDeviceId(device.getId()).orElse(null);
        if (monitor == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        device.setOnlineStatus(0);
        device.setLastOfflineTime(now);
        device.setUpdatedOn(now);
        deviceRepository.save(device);

        DeviceOnlineLogEntity log = new DeviceOnlineLogEntity();
        log.setId(idGenerator.nextId());
        log.setDeviceId(device.getId());
        log.setStatus(0);
        log.setChangeTime(now);
        log.setReason("offline inspection");
        log.setDeleted(0);
        log.setCreatedOn(now);
        deviceOnlineLogRepository.save(log);

        RuleEvaluationResult result = alarmRuleEngine.buildOfflineResult(
            monitor.getName() == null ? "监测对象" : monitor.getName(),
            offlineSeconds
        );
        alarmService.createOrMerge(
            DeviceResolverService.ResolvedTarget.forDevice(device, monitor),
            result,
            now,
            toJson(Collections.singletonMap("offlineSeconds", offlineSeconds)),
            "[]"
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize inspection detail", ex);
        }
    }
}
