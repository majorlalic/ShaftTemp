package com.example.demo.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.AppProperties;
import com.example.demo.service.AlarmService;
import com.example.demo.service.AlarmRuleEngine;
import com.example.demo.service.AlarmRuleResolverService;
import com.example.demo.entity.DeviceEntity;
import com.example.demo.entity.MonitorEntity;
import com.example.demo.dao.DeviceOnlineLogRepository;
import com.example.demo.dao.DeviceRepository;
import com.example.demo.dao.MonitorRepository;
import com.example.demo.service.RealtimeStateService;
import com.example.demo.service.IdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfflineInspectionTaskTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private DeviceOnlineLogRepository deviceOnlineLogRepository;
    @Mock
    private RealtimeStateService realtimeStateService;
    @Mock
    private AlarmRuleEngine alarmRuleEngine;
    @Mock
    private AlarmRuleResolverService alarmRuleResolverService;
    @Mock
    private AlarmService alarmService;
    @Mock
    private IdGenerator idGenerator;

    @Test
    void shouldSkipRecentDevice() {
        AppProperties properties = new AppProperties();
        properties.getAlarm().setOfflineThresholdSeconds(30);
        OfflineInspectionTask task = new OfflineInspectionTask(
            properties,
            deviceRepository,
            monitorRepository,
            deviceOnlineLogRepository,
            realtimeStateService,
            alarmRuleEngine,
            alarmRuleResolverService,
            alarmService,
            idGenerator,
            new ObjectMapper()
        );

        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        device.setLastReportTime(LocalDateTime.now());
        MonitorEntity monitor = new MonitorEntity();
        monitor.setId(10L);
        when(deviceRepository.findAllActive()).thenReturn(Collections.singletonList(device));
        when(monitorRepository.findActiveByDeviceId(1L)).thenReturn(Optional.of(monitor));
        when(realtimeStateService.getLastReportTime(1L)).thenReturn(Optional.of(LocalDateTime.now()));
        when(alarmRuleResolverService.resolveDeviceRule(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("DEVICE_OFFLINE")))
            .thenReturn(new AlarmRuleResolverService.RuleConfig(true, 2, java.math.BigDecimal.valueOf(30L), null));

        task.inspect();

        verify(alarmService, never()).createOrMerge(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }
}
