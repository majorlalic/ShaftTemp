package com.example.demo.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FiberBreakRule {

    public List<RuleEvaluationResult> evaluate(String monitorName, List<BigDecimal> values, BigDecimal threshold) {
        List<Integer> pointIndexes = new ArrayList<Integer>();
        BigDecimal triggerValue = null;
        for (int i = 0; i < values.size(); i++) {
            BigDecimal value = values.get(i);
            if (value.compareTo(threshold) <= 0) {
                pointIndexes.add(i + 1);
                if (triggerValue == null || value.compareTo(triggerValue) < 0) {
                    triggerValue = value;
                }
            }
        }
        if (pointIndexes.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new RuleEvaluationResult(
            "FIBER_BREAK",
            "REALTIME",
            3,
            monitorName + "断纤告警",
            "检测到温度点位低于断纤阈值" + threshold + "℃",
            pointIndexes,
            triggerValue,
            threshold
        ));
    }
}
