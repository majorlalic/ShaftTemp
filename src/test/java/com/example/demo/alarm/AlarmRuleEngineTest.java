package com.example.demo.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.demo.AppProperties;
import com.example.demo.alarm.rule.AlarmRuleEngine;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.alarm.rule.TemperatureThresholdRule;
import com.example.demo.persistence.entity.MonitorEntity;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlarmRuleEngineTest {

    @Test
    void shouldMatchTemperatureThreshold() {
        AppProperties properties = new AppProperties();
        properties.getAlarm().setTemperatureThreshold(new BigDecimal("70"));
        AlarmRuleEngine engine = new AlarmRuleEngine(new TemperatureThresholdRule(), properties);
        MonitorEntity monitor = new MonitorEntity();
        monitor.setName("1号竖井");

        List<RuleEvaluationResult> results = engine.evaluateRealtime(
            monitor,
            Arrays.asList(new BigDecimal("65"), new BigDecimal("72.5"), new BigDecimal("68"))
        );

        assertEquals(1, results.size());
        assertEquals("TEMP_THRESHOLD", results.get(0).getAlarmType());
        assertTrue(results.get(0).getPointIndexes().contains(2));
    }
}
