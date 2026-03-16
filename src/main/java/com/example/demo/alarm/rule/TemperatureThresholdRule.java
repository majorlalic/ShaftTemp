package com.example.demo.alarm.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TemperatureThresholdRule {

    public List<RuleEvaluationResult> evaluate(String monitorName, List<BigDecimal> values, BigDecimal threshold) {
        List<Integer> pointIndexes = new ArrayList<Integer>();
        BigDecimal maxValue = null;
        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = values.get(i);
            if (value.compareTo(threshold) > 0) {
                pointIndexes.add(i + 1);
                if (maxValue == null || value.compareTo(maxValue) > 0) {
                    maxValue = value;
                }
            }
        }
        if (pointIndexes.isEmpty()) {
            return Collections.emptyList();
        }
        String title = monitorName + "温度超限";
        String content = "检测到温度超过阈值" + threshold + "℃";
        RuleEvaluationResult result = new RuleEvaluationResult(
            "TEMP_THRESHOLD",
            "REALTIME",
            2,
            title,
            content,
            pointIndexes,
            maxValue,
            threshold
        );
        return Collections.singletonList(result);
    }
}
