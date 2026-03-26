package com.example.demo.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.alarm.AlarmStatus;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.persistence.entity.AlarmEntity;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.repository.AlarmJdbcRepository;
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
    private AlarmJdbcRepository alarmJdbcRepository;
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
        when(alarmJdbcRepository.upsertPendingAlarm(any(AlarmEntity.class))).thenReturn(true);
        AlarmEntity persisted = new AlarmEntity();
        persisted.setId(1001L);
        persisted.setMergeCount(1);
        persisted.setEventCount(0);
        persisted.setStatus(AlarmStatus.PENDING_CONFIRM);
        persisted.setMergeKey("10:TEMP_THRESHOLD");
        when(alarmRepository.findByMergeKey("10:TEMP_THRESHOLD")).thenReturn(Optional.of(persisted));
        when(idGenerator.nextId()).thenReturn(1001L, 1002L);
        when(alarmRepository.updateById(any(AlarmEntity.class))).thenReturn(1);

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
        existing.setMergeCount(3);
        existing.setEventCount(2);
        existing.setMergeKey("10:TEMP_THRESHOLD");
        existing.setStatus(AlarmStatus.PENDING_CONFIRM);

        when(realtimeStateService.shouldWriteMergedEvent("TEMP_THRESHOLD", "10", LocalDateTime.of(2026, 3, 13, 10, 0))).thenReturn(true);
        when(alarmJdbcRepository.upsertPendingAlarm(any(AlarmEntity.class))).thenReturn(false);
        when(alarmRepository.findByMergeKey("10:TEMP_THRESHOLD")).thenReturn(Optional.of(existing));
        when(alarmRepository.updateById(any(AlarmEntity.class))).thenReturn(1);
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
        verify(alarmRepository, atLeastOnce()).updateById(captor.capture());
        AlarmEntity lastUpdated = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(3, lastUpdated.getMergeCount().intValue());
        assertEquals(3, lastUpdated.getEventCount().intValue());
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

        when(realtimeStateService.shouldWriteMergedEvent("TEMP_THRESHOLD", "10", LocalDateTime.of(2026, 3, 13, 10, 0))).thenReturn(false);
        when(alarmJdbcRepository.upsertPendingAlarm(any(AlarmEntity.class))).thenReturn(false);
        when(alarmRepository.findByMergeKey("10:TEMP_THRESHOLD")).thenReturn(Optional.of(existing));

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
        verify(alarmJdbcRepository).upsertPendingAlarm(any(AlarmEntity.class));
    }

    @Test
    void shouldCreateSinglePendingAlarmUnderConcurrentUpserts() throws InterruptedException {
        AlarmEntity stored = new AlarmEntity();
        stored.setId(500L);
        stored.setMergeKey("10:TEMP_THRESHOLD");
        stored.setStatus(AlarmStatus.PENDING_CONFIRM);
        stored.setMergeCount(0);
        stored.setEventCount(0);

        when(realtimeStateService.shouldWriteMergedEvent("TEMP_THRESHOLD", "10", LocalDateTime.of(2026, 3, 13, 10, 0))).thenReturn(false);
        when(idGenerator.nextId()).thenReturn(1001L, 1002L, 1003L, 1004L);
        when(alarmRepository.findByMergeKey("10:TEMP_THRESHOLD")).thenAnswer(invocation -> Optional.of(stored));
        when(alarmRepository.updateById(any(AlarmEntity.class))).thenReturn(1);

        java.util.concurrent.atomic.AtomicBoolean inserted = new java.util.concurrent.atomic.AtomicBoolean(false);
        doAnswer(invocation -> {
            AlarmEntity candidate = invocation.getArgument(0);
            if (inserted.compareAndSet(false, true)) {
                stored.setId(candidate.getId());
                stored.setAlarmCode(candidate.getAlarmCode());
                stored.setAlarmType(candidate.getAlarmType());
                stored.setMergeKey(candidate.getMergeKey());
                stored.setStatus(candidate.getStatus());
                stored.setMonitorId(candidate.getMonitorId());
                stored.setDeviceId(candidate.getDeviceId());
                stored.setMergeCount(1);
                stored.setEventCount(0);
                return true;
            }
            stored.setMergeCount(stored.getMergeCount() + 1);
            return false;
        }).when(alarmJdbcRepository).upsertPendingAlarm(any(AlarmEntity.class));

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

        Thread first = new Thread(() -> alarmMergeService.createOrMerge(
            resolved, result, LocalDateTime.of(2026, 3, 13, 10, 0), "{}", "[3]"
        ));
        Thread second = new Thread(() -> alarmMergeService.createOrMerge(
            resolved, result, LocalDateTime.of(2026, 3, 13, 10, 0), "{}", "[3]"
        ));
        first.start();
        second.start();
        first.join();
        second.join();

        assertEquals(2, stored.getMergeCount().intValue());
        verify(eventJdbcRepository, atLeastOnce()).insert(any());
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
        when(alarmRepository.updateById(any(AlarmEntity.class))).thenReturn(1);
        when(idGenerator.nextId()).thenReturn(1004L);

        AlarmEntity alarm = alarmMergeService.confirm(500L, 99L, "checked");

        assertEquals(AlarmStatus.CONFIRMED, alarm.getStatus().intValue());
        assertEquals(99L, alarm.getHandler().longValue());
        assertNotNull(alarm.getHandleTime());
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
        when(alarmRepository.updateById(any(AlarmEntity.class))).thenReturn(1);
        when(idGenerator.nextId()).thenReturn(1005L);

        AlarmEntity alarm = alarmMergeService.close(500L, "done");

        assertEquals(AlarmStatus.CLOSED, alarm.getStatus().intValue());
        assertEquals(2, alarm.getEventCount().intValue());
        verify(realtimeStateService).clearActiveAlarmId("TEMP_THRESHOLD", "10");
        verify(eventJdbcRepository).insert(any());
    }

}
