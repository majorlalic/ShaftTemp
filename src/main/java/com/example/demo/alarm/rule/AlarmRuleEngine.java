package com.example.demo.alarm.rule;

import com.example.demo.AppProperties;
import com.example.demo.persistence.entity.MonitorEntity;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlarmRuleEngine {

    private final TemperatureThresholdRule temperatureThresholdRule;
    private final AppProperties appProperties;

    public AlarmRuleEngine(TemperatureThresholdRule temperatureThresholdRule, AppProperties appProperties) {
        this.temperatureThresholdRule = temperatureThresholdRule;
        this.appProperties = appProperties;
    }

    public List<RuleEvaluationResult> evaluateRealtime(MonitorEntity monitor, List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return temperatureThresholdRule.evaluate(
            monitor.getName() == null ? "监测对象" : monitor.getName(),
            values,
            appProperties.getAlarm().getTemperatureThreshold()
        );
    }

    public RuleEvaluationResult buildOfflineResult(String monitorName, long offlineSeconds) {
        return new RuleEvaluationResult(
            "DEVICE_OFFLINE",
            "INSPECTION",
            2,
            monitorName + "设备离线",
            "设备连续" + offlineSeconds + "秒未上报",
            Collections.<Integer>emptyList(),
            BigDecimal.valueOf(offlineSeconds),
            BigDecimal.valueOf(appProperties.getAlarm().getOfflineThresholdSeconds())
        );
    }
}
