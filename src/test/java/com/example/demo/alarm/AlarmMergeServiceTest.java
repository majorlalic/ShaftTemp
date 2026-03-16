package com.example.demo.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.persistence.entity.AlarmEntity;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.repository.AlarmRepository;
import com.example.demo.persistence.repository.EventRepository;
import com.example.demo.realtime.RealtimeStateService;
import com.example.demo.support.IdGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmMergeServiceTest {

    @Mock
    private AlarmRepository alarmRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private RealtimeStateService realtimeStateService;
    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private AlarmMergeService alarmMergeService;

    @Test
    void shouldCreateNewAlarmWhenNoActiveAlarmExists() {
        when(realtimeStateService.getActiveAlarmId("TEMP_THRESHOLD", 10L)).thenReturn(Optional.empty());
        when(alarmRepository.findActiveAlarm(10L, "TEMP_THRESHOLD")).thenReturn(Optional.empty());
        when(idGenerator.nextId()).thenReturn(1001L, 1002L);
        when(alarmRepository.save(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        MonitorEntity monitor = new MonitorEntity();
        monitor.setId(10L);
        RuleEvaluationResult result = new RuleEvaluationResult(
            "TEMP_THRESHOLD", "REALTIME", 2, "温度超限", "超过阈值",
            Collections.singletonList(3), new BigDecimal("90.0"), new BigDecimal("70.0")
        );

        AlarmEntity alarm = alarmMergeService.createOrMerge(
            device, monitor, result, LocalDateTime.of(2026, 3, 13, 10, 0), "{}", "[3]"
        );

        assertNotNull(alarm);
        assertEquals(1, alarm.getMergeCount().intValue());
        verify(eventRepository).save(any());
    }

    @Test
    void shouldMergeIntoExistingAlarm() {
        AlarmEntity existing = new AlarmEntity();
        existing.setId(500L);
        existing.setMergeCount(2);
        existing.setStatus("ACTIVE");

        when(realtimeStateService.getActiveAlarmId("TEMP_THRESHOLD", 10L)).thenReturn(Optional.of(500L));
        when(alarmRepository.findById(500L)).thenReturn(Optional.of(existing));
        when(alarmRepository.save(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idGenerator.nextId()).thenReturn(1003L);

        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        MonitorEntity monitor = new MonitorEntity();
        monitor.setId(10L);
        RuleEvaluationResult result = new RuleEvaluationResult(
            "TEMP_THRESHOLD", "REALTIME", 2, "温度超限", "超过阈值",
            Collections.singletonList(3), new BigDecimal("90.0"), new BigDecimal("70.0")
        );

        AlarmEntity alarm = alarmMergeService.createOrMerge(
            device, monitor, result, LocalDateTime.of(2026, 3, 13, 10, 0), "{}", "[3]"
        );

        assertEquals(3, alarm.getMergeCount().intValue());
        ArgumentCaptor<AlarmEntity> captor = ArgumentCaptor.forClass(AlarmEntity.class);
        verify(alarmRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getMergeCount().intValue());
    }
}
