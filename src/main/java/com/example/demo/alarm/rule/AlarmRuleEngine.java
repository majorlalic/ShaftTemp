package com.example.demo.alarm.rule;

import com.example.demo.AppProperties;
import com.example.demo.alarm.rule.service.AlarmRuleResolverService;
import com.example.demo.ingest.service.ReportIngestService.ReportMetrics;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.realtime.RealtimeStateService.RealtimeSummary;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlarmRuleEngine {

    private final AppProperties appProperties;
    private final AlarmRuleResolverService alarmRuleResolverService;

    public AlarmRuleEngine(AppProperties appProperties, AlarmRuleResolverService alarmRuleResolverService) {
        this.appProperties = appProperties;
        this.alarmRuleResolverService = alarmRuleResolverService;
    }

    public List<RuleEvaluationResult> evaluateMeasure(
        DeviceResolverService.ResolvedTarget resolved,
        ReportMetrics metrics,
        RealtimeSummary previousSummary
    ) {
        if (metrics == null) {
            return Collections.emptyList();
        }
        String monitorName = resolved.getMonitor().getName() == null ? "监测对象" : resolved.getMonitor().getName();
        String displayName = resolved.getPartitionName() == null ? monitorName : monitorName + "-" + resolved.getPartitionName();
        List<RuleEvaluationResult> results = new ArrayList<RuleEvaluationResult>();
        AlarmRuleResolverService.RuleConfig thresholdRule = alarmRuleResolverService.resolveMonitorRule(resolved, "TEMP_THRESHOLD");
        if (thresholdRule.isEnabled()
            && thresholdRule.getThresholdValue() != null
            && metrics.getMaxTemp().compareTo(thresholdRule.getThresholdValue()) > 0) {
            results.add(new RuleEvaluationResult(
                "TEMP_THRESHOLD",
                "REALTIME",
                thresholdRule.getLevel(),
                displayName + "温度超限",
                "最大温度" + metrics.getMaxTemp() + "超过阈值" + thresholdRule.getThresholdValue(),
                Collections.<Integer>emptyList(),
                metrics.getMaxTemp(),
                thresholdRule.getThresholdValue()
            ));
        }
        BigDecimal diff = metrics.getMaxTemp().subtract(metrics.getMinTemp());
        AlarmRuleResolverService.RuleConfig diffRule = alarmRuleResolverService.resolveMonitorRule(resolved, "TEMP_DIFFERENCE");
        if (diffRule.isEnabled()
            && diffRule.getThresholdValue() != null
            && diff.compareTo(diffRule.getThresholdValue()) > 0) {
            results.add(new RuleEvaluationResult(
                "TEMP_DIFFERENCE",
                "REALTIME",
                diffRule.getLevel(),
                displayName + "差温异常",
                "分区差温" + diff + "超过阈值" + diffRule.getThresholdValue(),
                Collections.<Integer>emptyList(),
                diff,
                diffRule.getThresholdValue()
            ));
        }
        if (previousSummary != null && previousSummary.getMaxTemp() != null) {
            BigDecimal riseValue = metrics.getMaxTemp().subtract(previousSummary.getMaxTemp());
            AlarmRuleResolverService.RuleConfig riseRule = alarmRuleResolverService.resolveMonitorRule(resolved, "TEMP_RISE_RATE");
            if (riseRule.isEnabled()
                && riseRule.getThresholdValue() != null
                && riseValue.compareTo(riseRule.getThresholdValue()) > 0) {
                results.add(new RuleEvaluationResult(
                    "TEMP_RISE_RATE",
                    "REALTIME",
                    riseRule.getLevel(),
                    displayName + "升温过快",
                    "最大温度较上次上升" + riseValue,
                    Collections.<Integer>emptyList(),
                    riseValue,
                    riseRule.getThresholdValue()
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

    public RuleEvaluationResult buildOfflineResult(DeviceResolverService.ResolvedTarget resolved, long offlineSeconds) {
        AlarmRuleResolverService.RuleConfig rule = alarmRuleResolverService.resolveDeviceRule(resolved, "DEVICE_OFFLINE");
        if (!rule.isEnabled()) {
            return null;
        }
        String monitorName = resolved.getMonitor().getName() == null ? "监测对象" : resolved.getMonitor().getName();
        return new RuleEvaluationResult(
            "DEVICE_OFFLINE",
            "INSPECTION",
            rule.getLevel(),
            monitorName + "设备离线",
            "设备连续" + offlineSeconds + "秒未上报",
            Collections.<Integer>emptyList(),
            BigDecimal.valueOf(offlineSeconds),
            rule.getThresholdValue() == null ? BigDecimal.valueOf(appProperties.getAlarm().getOfflineThresholdSeconds()) : rule.getThresholdValue()
        );
    }

    public RuleEvaluationResult buildPartitionFaultResult(DeviceResolverService.ResolvedTarget resolved, String message) {
        AlarmRuleResolverService.RuleConfig rule = alarmRuleResolverService.resolveDeviceRule(resolved, "PARTITION_FAULT");
        if (!rule.isEnabled()) {
            return null;
        }
        return buildUpstreamAlarmResult("PARTITION_FAULT", resolved.getMonitor().getName(), resolved.getPartitionName(), message, rule.getLevel());
    }

    public RuleEvaluationResult buildUpstreamAlarmResult(String alarmType, String monitorName, String partitionName, String message, int level) {
        String prefix = monitorName == null ? "监测对象" : monitorName;
        if (partitionName != null && !partitionName.trim().isEmpty()) {
            prefix = prefix + "-" + partitionName;
        }
        return new RuleEvaluationResult(
            alarmType,
            "MQ_STATUS",
            level,
            prefix + "状态告警",
            message,
            Collections.<Integer>emptyList(),
            BigDecimal.ONE,
            BigDecimal.ONE
        );
    }
}
