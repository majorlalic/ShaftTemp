package com.example.demo.alarm.rule;

import com.example.demo.AppProperties;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.realtime.RealtimeStateService.RealtimeSummary;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlarmRuleEngine {

    private final TemperatureThresholdRule temperatureThresholdRule;
    private final TemperatureDifferenceRule temperatureDifferenceRule;
    private final TemperatureRiseRateRule temperatureRiseRateRule;
    private final FiberBreakRule fiberBreakRule;
    private final AppProperties appProperties;

    public AlarmRuleEngine(
        TemperatureThresholdRule temperatureThresholdRule,
        TemperatureDifferenceRule temperatureDifferenceRule,
        TemperatureRiseRateRule temperatureRiseRateRule,
        FiberBreakRule fiberBreakRule,
        AppProperties appProperties
    ) {
        this.temperatureThresholdRule = temperatureThresholdRule;
        this.temperatureDifferenceRule = temperatureDifferenceRule;
        this.temperatureRiseRateRule = temperatureRiseRateRule;
        this.fiberBreakRule = fiberBreakRule;
        this.appProperties = appProperties;
    }

    public List<RuleEvaluationResult> evaluateRealtime(MonitorEntity monitor, List<BigDecimal> values, RealtimeSummary previousSummary) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        String monitorName = monitor.getName() == null ? "监测对象" : monitor.getName();
        BigDecimal currentMaxTemp = values.get(0);
        for (BigDecimal value : values) {
            if (value.compareTo(currentMaxTemp) > 0) {
                currentMaxTemp = value;
            }
        }
        List<RuleEvaluationResult> results = new ArrayList<RuleEvaluationResult>();
        results.addAll(temperatureThresholdRule.evaluate(monitorName, values, appProperties.getAlarm().getTemperatureThreshold()));
        results.addAll(temperatureDifferenceRule.evaluate(monitorName, values, appProperties.getAlarm().getTemperatureDiffThreshold()));
        results.addAll(temperatureRiseRateRule.evaluate(monitorName, currentMaxTemp, previousSummary, appProperties.getAlarm().getRiseRateThreshold()));
        results.addAll(fiberBreakRule.evaluate(monitorName, values, appProperties.getAlarm().getFiberBreakThreshold()));
        return results;
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
