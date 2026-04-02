package com.example.demo.service;

import com.example.demo.service.RealtimeStateService.RealtimeSummary;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TemperatureRiseRateRule {

    public List<RuleEvaluationResult> evaluate(
        String monitorName,
        BigDecimal currentMaxTemp,
        RealtimeSummary previousSummary,
        BigDecimal threshold
    ) {
        if (previousSummary == null || previousSummary.getMaxTemp() == null) {
            return Collections.emptyList();
        }
        BigDecimal rise = currentMaxTemp.subtract(previousSummary.getMaxTemp());
        if (rise.compareTo(threshold) <= 0) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new RuleEvaluationResult(
            "TEMP_RISE_RATE",
            "REALTIME",
            2,
            monitorName + "升温速率异常",
            "检测到升温超过阈值" + threshold + "℃",
            Collections.<Integer>emptyList(),
            rise,
            threshold
        ));
    }
}
