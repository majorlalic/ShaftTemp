package com.example.demo.alarm.rule;

import com.example.demo.AppProperties;
import com.example.demo.ingest.service.ReportIngestService.ReportMetrics;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.realtime.RealtimeStateService.RealtimeSummary;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlarmRuleEngine {

    private final AppProperties appProperties;

    public AlarmRuleEngine(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public List<RuleEvaluationResult> evaluateMeasure(
        MonitorEntity monitor,
        String partitionName,
        ReportMetrics metrics,
        RealtimeSummary previousSummary
    ) {
        if (metrics == null) {
            return Collections.emptyList();
        }
        String monitorName = monitor.getName() == null ? "监测对象" : monitor.getName();
        String displayName = partitionName == null ? monitorName : monitorName + "-" + partitionName;
        List<RuleEvaluationResult> results = new ArrayList<RuleEvaluationResult>();
        if (metrics.getMaxTemp().compareTo(appProperties.getAlarm().getTemperatureThreshold()) > 0) {
            results.add(new RuleEvaluationResult(
                "TEMP_THRESHOLD",
                "REALTIME",
                2,
                displayName + "温度超限",
                "最大温度" + metrics.getMaxTemp() + "超过阈值" + appProperties.getAlarm().getTemperatureThreshold(),
                Collections.<Integer>emptyList(),
                metrics.getMaxTemp(),
                appProperties.getAlarm().getTemperatureThreshold()
            ));
        }
        BigDecimal diff = metrics.getMaxTemp().subtract(metrics.getMinTemp());
        if (diff.compareTo(appProperties.getAlarm().getTemperatureDiffThreshold()) > 0) {
            results.add(new RuleEvaluationResult(
                "TEMP_DIFFERENCE",
                "REALTIME",
                2,
                displayName + "差温异常",
                "分区差温" + diff + "超过阈值" + appProperties.getAlarm().getTemperatureDiffThreshold(),
                Collections.<Integer>emptyList(),
                diff,
                appProperties.getAlarm().getTemperatureDiffThreshold()
            ));
        }
        if (previousSummary != null && previousSummary.getMaxTemp() != null) {
            BigDecimal riseValue = metrics.getMaxTemp().subtract(previousSummary.getMaxTemp());
            if (riseValue.compareTo(appProperties.getAlarm().getRiseRateThreshold()) > 0) {
                results.add(new RuleEvaluationResult(
                    "TEMP_RISE_RATE",
                    "REALTIME",
                    2,
                    displayName + "升温过快",
                    "最大温度较上次上升" + riseValue,
                    Collections.<Integer>emptyList(),
                    riseValue,
                    appProperties.getAlarm().getRiseRateThreshold()
                ));
            }
        }
        return results;
    }

    public RuleEvaluationResult buildUpstreamAlarmResult(String alarmType, String monitorName, String partitionName, String message) {
        String prefix = monitorName == null ? "监测对象" : monitorName;
        if (partitionName != null && !partitionName.trim().isEmpty()) {
            prefix = prefix + "-" + partitionName;
        }
        return new RuleEvaluationResult(
            alarmType,
            "MQ_STATUS",
            2,
            prefix + "状态告警",
            message,
            Collections.<Integer>emptyList(),
            BigDecimal.ONE,
            BigDecimal.ONE
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
