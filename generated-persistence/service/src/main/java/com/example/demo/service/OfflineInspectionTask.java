package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.example.demo.service.AlarmService;
import com.example.demo.service.AlarmRuleEngine;
import com.example.demo.service.RuleEvaluationResult;
import com.example.demo.service.AlarmRuleResolverService;
import com.example.demo.service.DeviceResolverService;
import com.csg.dgri.szsiom.sysmanage.model.DeviceVO;
import com.csg.dgri.szsiom.sysmanage.model.DeviceOnlineLogVO;
import com.csg.dgri.szsiom.sysmanage.model.MonitorVO;
import com.csg.dgri.szsiom.sysmanage.appservice.DeviceOnlineLogAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.DeviceAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.MonitorAppService;
import com.example.demo.service.RealtimeStateService;
import com.example.demo.service.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OfflineInspectionTask {

    private final AppProperties appProperties;
    private final DeviceAppService<?> deviceRepository;
    private final MonitorAppService<?> monitorRepository;
    private final DeviceOnlineLogAppService<?> deviceOnlineLogRepository;
    private final RealtimeStateService realtimeStateService;
    private final AlarmRuleEngine alarmRuleEngine;
    private final AlarmRuleResolverService alarmRuleResolverService;
    private final AlarmService alarmService;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public OfflineInspectionTask(
        AppProperties appProperties,
        DeviceAppService<?> deviceRepository,
        MonitorAppService<?> monitorRepository,
        DeviceOnlineLogAppService<?> deviceOnlineLogRepository,
        RealtimeStateService realtimeStateService,
        AlarmRuleEngine alarmRuleEngine,
        AlarmRuleResolverService alarmRuleResolverService,
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
        this.alarmRuleResolverService = alarmRuleResolverService;
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
        List<DeviceVO> devices = deviceRepository.findAllActive();
        Map<Long, MonitorVO> monitorByDeviceId = loadMonitorByDeviceId(devices);
        for (DeviceVO device : devices) {
            MonitorVO monitor = monitorByDeviceId.get(device.getId());
            if (monitor == null) {
                continue;
            }
            DeviceResolverService.ResolvedTarget resolved = DeviceResolverService.ResolvedTarget.forDevice(device, monitor);
            Optional<LocalDateTime> reportTime = realtimeStateService.getLastReportTime(device.getId());
            LocalDateTime lastReportTime = reportTime.orElse(device.getLastReportTime());
            if (lastReportTime == null) {
                continue;
            }
            long offlineSeconds = Duration.between(lastReportTime, LocalDateTime.now()).getSeconds();
            AlarmRuleResolverService.RuleConfig rule = alarmRuleResolverService.resolveDeviceRule(resolved, "DEVICE_OFFLINE");
            long thresholdSeconds = rule.getThresholdValue() == null
                ? appProperties.getAlarm().getOfflineThresholdSeconds()
                : rule.getThresholdValue().longValue();
            if (!rule.isEnabled() || offlineSeconds < thresholdSeconds) {
                continue;
            }
            int level = realtimeStateService.incrementOfflineLevel(device.getId());
            if (level > 1 && device.getOnlineStatus() != null && device.getOnlineStatus() == 0) {
                continue;
            }
            triggerOffline(resolved, offlineSeconds);
        }
    }

    private Map<Long, MonitorVO> loadMonitorByDeviceId(List<DeviceVO> devices) {
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> deviceIds = new java.util.ArrayList<Long>(devices.size());
        for (DeviceVO device : devices) {
            if (device != null && device.getId() != null) {
                deviceIds.add(device.getId());
            }
        }
        if (deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MonitorVO> monitors = monitorRepository.findActiveByDeviceIds(deviceIds);
        Map<Long, MonitorVO> monitorByDeviceId = new HashMap<Long, MonitorVO>();
        for (MonitorVO monitor : monitors) {
            if (monitor.getDeviceId() != null && !monitorByDeviceId.containsKey(monitor.getDeviceId())) {
                monitorByDeviceId.put(monitor.getDeviceId(), monitor);
            }
        }
        return monitorByDeviceId;
    }

    private void triggerOffline(DeviceResolverService.ResolvedTarget resolved, long offlineSeconds) {
        DeviceVO device = resolved.getDevice();
        MonitorVO monitor = resolved.getMonitor();
        LocalDateTime now = LocalDateTime.now();
        device.setOnlineStatus(0);
        device.setLastOfflineTime(now);
        device.setUpdatedOn(now);
        deviceRepository.updateById(device);

        DeviceOnlineLogVO log = new DeviceOnlineLogVO();
        log.setId(idGenerator.nextId());
        log.setDeviceId(device.getId());
        log.setStatus(0);
        log.setChangeTime(now);
        log.setReason("offline inspection");
        log.setDeleted(0);
        log.setCreatedOn(now);
        deviceOnlineLogRepository.insert(log);

        RuleEvaluationResult result = alarmRuleEngine.buildOfflineResult(resolved, offlineSeconds);
        if (result != null) {
            alarmService.createOrMerge(
                resolved,
                result,
                now,
                toJson(Collections.singletonMap("offlineSeconds", offlineSeconds)),
                "[]"
            );
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize inspection detail", ex);
        }
    }
}
