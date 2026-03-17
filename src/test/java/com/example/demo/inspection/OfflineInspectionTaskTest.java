package com.example.demo.inspection;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.AppProperties;
import com.example.demo.alarm.AlarmService;
import com.example.demo.alarm.rule.AlarmRuleEngine;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.repository.DeviceOnlineLogRepository;
import com.example.demo.persistence.repository.DeviceRepository;
import com.example.demo.persistence.repository.MonitorRepository;
import com.example.demo.realtime.RealtimeStateService;
import com.example.demo.support.IdGenerator;
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
            alarmService,
            idGenerator,
            new ObjectMapper()
        );

        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        device.setLastReportTime(LocalDateTime.now());
        when(deviceRepository.findAllActive()).thenReturn(Collections.singletonList(device));
        when(realtimeStateService.getLastReportTime(1L)).thenReturn(Optional.of(LocalDateTime.now()));

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
