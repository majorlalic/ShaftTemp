package com.example.demo.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.demo.AppProperties;
import com.example.demo.alarm.rule.AlarmRuleEngine;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.alarm.rule.service.AlarmRuleResolverService;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.ingest.service.ReportIngestService;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmRuleEngineTest {

    @Mock
    private AlarmRuleResolverService alarmRuleResolverService;

    @Test
    void shouldMatchTemperatureThreshold() {
        AppProperties properties = new AppProperties();
        properties.getAlarm().setTemperatureThreshold(new BigDecimal("70"));
        AlarmRuleEngine engine = new AlarmRuleEngine(properties, alarmRuleResolverService);
        MonitorEntity monitor = new MonitorEntity();
        monitor.setName("1号竖井");
        DeviceEntity device = new DeviceEntity();
        device.setId(3001L);
        DeviceResolverService.ResolvedTarget resolved = new DeviceResolverService.ResolvedTarget(
            device,
            monitor,
            null,
            "p1",
            "一区",
            "/TMP/p1",
            "dev-01",
            1,
            "MQ_PARTITION"
        );
        when(alarmRuleResolverService.resolveMonitorRule(resolved, "TEMP_THRESHOLD"))
            .thenReturn(new AlarmRuleResolverService.RuleConfig(true, 2, new BigDecimal("70"), null));
        when(alarmRuleResolverService.resolveMonitorRule(resolved, "TEMP_DIFFERENCE"))
            .thenReturn(new AlarmRuleResolverService.RuleConfig(false, 2, null, null));

        List<RuleEvaluationResult> results = engine.evaluateMeasure(
            resolved,
            new ReportIngestService.ReportMetrics(new BigDecimal("72.5"), new BigDecimal("65"), new BigDecimal("68.50")),
            null
        );

        assertEquals(1, results.size());
        assertEquals("TEMP_THRESHOLD", results.get(0).getAlarmType());
        assertTrue(results.get(0).getTitle().contains("一区"));
    }
}
