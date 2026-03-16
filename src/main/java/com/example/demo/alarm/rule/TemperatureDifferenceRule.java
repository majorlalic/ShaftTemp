package com.example.demo.alarm.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TemperatureDifferenceRule {

    public List<RuleEvaluationResult> evaluate(String monitorName, List<BigDecimal> values, BigDecimal threshold) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        BigDecimal maxValue = values.get(0);
        BigDecimal minValue = values.get(0);
        List<Integer> pointIndexes = new ArrayList<Integer>();
        int maxIndex = 1;
        int minIndex = 1;
        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = values.get(i);
            if (value.compareTo(maxValue) > 0) {
                maxValue = value;
                maxIndex = i + 1;
            }
            if (value.compareTo(minValue) < 0) {
                minValue = value;
                minIndex = i + 1;
            }
        }
        BigDecimal diff = maxValue.subtract(minValue);
        if (diff.compareTo(threshold) <= 0) {
            return Collections.emptyList();
        }
        pointIndexes.add(maxIndex);
        if (minIndex != maxIndex) {
            pointIndexes.add(minIndex);
        }
        return Collections.singletonList(new RuleEvaluationResult(
            "TEMP_DIFFERENCE",
            "REALTIME",
            2,
            monitorName + "差温异常",
            "检测到差温超过阈值" + threshold + "℃",
            pointIndexes,
            diff,
            threshold
        ));
    }
}
