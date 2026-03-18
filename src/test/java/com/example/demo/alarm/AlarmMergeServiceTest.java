package com.example.demo.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.alarm.AlarmStatus;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.persistence.entity.AlarmEntity;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.repository.AlarmRepository;
import com.example.demo.persistence.repository.EventRepository;
import com.example.demo.persistence.repository.EventJdbcRepository;
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
    private EventJdbcRepository eventJdbcRepository;
    @Mock
    private RealtimeStateService realtimeStateService;
    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private AlarmMergeService alarmMergeService;

    @Test
    void shouldCreateNewAlarmWhenNoActiveAlarmExists() {
        when(realtimeStateService.getActiveAlarmId("TEMP_THRESHOLD", "10")).thenReturn(Optional.empty());
        when(alarmRepository.findByMergeKey("10:TEMP_THRESHOLD")).thenReturn(Optional.empty());
        when(idGenerator.nextId()).thenReturn(1001L, 1002L);
        when(alarmRepository.saveAndFlush(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        MonitorEntity monitor = new MonitorEntity();
        monitor.setId(10L);
        DeviceResolverService.ResolvedTarget resolved = new DeviceResolverService.ResolvedTarget(
            device,
            monitor,
            null,
            "dev_TMP_th01",
            "一区",
            "/TMP/dev_TMP_th01",
            "dev",
            1,
            "MQ_PARTITION"
        );
        RuleEvaluationResult result = new RuleEvaluationResult(
            "TEMP_THRESHOLD", "REALTIME", 2, "温度超限", "超过阈值",
            Collections.singletonList(3), new BigDecimal("90.0"), new BigDecimal("70.0")
        );

        AlarmEntity alarm = alarmMergeService.createOrMerge(
            resolved, result, LocalDateTime.of(2026, 3, 13, 10, 0), "{}", "[3]"
        );

        assertNotNull(alarm);
        assertEquals(1, alarm.getMergeCount().intValue());
        assertEquals(1, alarm.getEventCount().intValue());
        assertEquals(AlarmStatus.PENDING_CONFIRM, alarm.getStatus().intValue());
        verify(eventJdbcRepository).insert(any());
    }

    @Test
    void shouldMergeIntoExistingAlarm() {
        AlarmEntity existing = new AlarmEntity();
        existing.setId(500L);
        existing.setMergeCount(2);
        existing.setEventCount(2);
        existing.setMergeKey("10:TEMP_THRESHOLD");
        existing.setStatus(AlarmStatus.PENDING_CONFIRM);

        when(realtimeStateService.getActiveAlarmId("TEMP_THRESHOLD", "10")).thenReturn(Optional.of(500L));
        when(realtimeStateService.shouldWriteMergedEvent("TEMP_THRESHOLD", "10", LocalDateTime.of(2026, 3, 13, 10, 0))).thenReturn(true);
        when(alarmRepository.findById(500L)).thenReturn(Optional.of(existing));
        when(alarmRepository.saveAndFlush(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idGenerator.nextId()).thenReturn(1003L);

        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        MonitorEntity monitor = new MonitorEntity();
        monitor.setId(10L);
        DeviceResolverService.ResolvedTarget resolved = new DeviceResolverService.ResolvedTarget(
            device,
            monitor,
            null,
            "dev_TMP_th01",
            "一区",
            "/TMP/dev_TMP_th01",
            "dev",
            1,
            "MQ_PARTITION"
        );
        RuleEvaluationResult result = new RuleEvaluationResult(
            "TEMP_THRESHOLD", "REALTIME", 2, "温度超限", "超过阈值",
            Collections.singletonList(3), new BigDecimal("90.0"), new BigDecimal("70.0")
        );

        AlarmEntity alarm = alarmMergeService.createOrMerge(
            resolved, result, LocalDateTime.of(2026, 3, 13, 10, 0), "{}", "[3]"
        );

        assertEquals(3, alarm.getMergeCount().intValue());
        assertEquals(3, alarm.getEventCount().intValue());
        ArgumentCaptor<AlarmEntity> captor = ArgumentCaptor.forClass(AlarmEntity.class);
        verify(alarmRepository, atLeastOnce()).saveAndFlush(captor.capture());
        AlarmEntity lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(3, lastSaved.getMergeCount().intValue());
        assertEquals(3, lastSaved.getEventCount().intValue());
        verify(eventJdbcRepository).insert(any());
    }

    @Test
    void shouldSkipMergedEventWhenThrottled() {
        AlarmEntity existing = new AlarmEntity();
        existing.setId(500L);
        existing.setMergeCount(2);
        existing.setEventCount(2);
        existing.setMergeKey("10:TEMP_THRESHOLD");
        existing.setStatus(AlarmStatus.PENDING_CONFIRM);

        when(realtimeStateService.getActiveAlarmId("TEMP_THRESHOLD", "10")).thenReturn(Optional.of(500L));
        when(realtimeStateService.shouldWriteMergedEvent("TEMP_THRESHOLD", "10", LocalDateTime.of(2026, 3, 13, 10, 0))).thenReturn(false);
        when(alarmRepository.findById(500L)).thenReturn(Optional.of(existing));
        when(alarmRepository.saveAndFlush(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceEntity device = new DeviceEntity();
        device.setId(1L);
        MonitorEntity monitor = new MonitorEntity();
        monitor.setId(10L);
        DeviceResolverService.ResolvedTarget resolved = new DeviceResolverService.ResolvedTarget(
            device,
            monitor,
            null,
            "dev_TMP_th01",
            "一区",
            "/TMP/dev_TMP_th01",
            "dev",
            1,
            "MQ_PARTITION"
        );
        RuleEvaluationResult result = new RuleEvaluationResult(
            "TEMP_THRESHOLD", "REALTIME", 2, "温度超限", "超过阈值",
            Collections.singletonList(3), new BigDecimal("90.0"), new BigDecimal("70.0")
        );

        alarmMergeService.createOrMerge(resolved, result, LocalDateTime.of(2026, 3, 13, 10, 0), "{}", "[3]");

        verify(eventJdbcRepository, never()).insert(any());
        verify(alarmRepository).saveAndFlush(any(AlarmEntity.class));
    }

    @Test
    void shouldConfirmAlarm() {
        AlarmEntity existing = new AlarmEntity();
        existing.setId(500L);
        existing.setAlarmType("TEMP_THRESHOLD");
        existing.setMonitorId(10L);
        existing.setDeviceId(1L);
        existing.setMergeCount(1);
        existing.setEventCount(1);
        existing.setAlarmLevel(2);
        when(alarmRepository.findById(500L)).thenReturn(Optional.of(existing));
        when(alarmRepository.saveAndFlush(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idGenerator.nextId()).thenReturn(1004L);

        AlarmEntity alarm = alarmMergeService.confirm(500L, 99L, "checked");

        assertEquals(AlarmStatus.CONFIRMED, alarm.getStatus().intValue());
        assertEquals(99L, alarm.getConfirmUserId().longValue());
        assertEquals(2, alarm.getEventCount().intValue());
        verify(realtimeStateService).clearActiveAlarmId("TEMP_THRESHOLD", "10");
        verify(eventJdbcRepository).insert(any());
    }

    @Test
    void shouldCloseAlarm() {
        AlarmEntity existing = new AlarmEntity();
        existing.setId(500L);
        existing.setAlarmType("TEMP_THRESHOLD");
        existing.setMonitorId(10L);
        existing.setDeviceId(1L);
        existing.setMergeCount(1);
        existing.setEventCount(1);
        existing.setAlarmLevel(2);
        when(alarmRepository.findById(500L)).thenReturn(Optional.of(existing));
        when(alarmRepository.saveAndFlush(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idGenerator.nextId()).thenReturn(1005L);

        AlarmEntity alarm = alarmMergeService.close(500L, "done");

        assertEquals(AlarmStatus.CLOSED, alarm.getStatus().intValue());
        assertEquals(2, alarm.getEventCount().intValue());
        verify(realtimeStateService).clearActiveAlarmId("TEMP_THRESHOLD", "10");
        verify(eventJdbcRepository).insert(any());
    }

}
