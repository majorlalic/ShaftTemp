package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;

public class RuleEvaluationResult {

    private final String alarmType;
    private final String sourceType;
    private final int alarmLevel;
    private final String title;
    private final String content;
    private final List<Integer> pointIndexes;
    private final BigDecimal triggerValue;
    private final BigDecimal threshold;

    public RuleEvaluationResult(
        String alarmType,
        String sourceType,
        int alarmLevel,
        String title,
        String content,
        List<Integer> pointIndexes,
        BigDecimal triggerValue,
        BigDecimal threshold
    ) {
        this.alarmType = alarmType;
        this.sourceType = sourceType;
        this.alarmLevel = alarmLevel;
        this.title = title;
        this.content = content;
        this.pointIndexes = pointIndexes;
        this.triggerValue = triggerValue;
        this.threshold = threshold;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public int getAlarmLevel() {
        return alarmLevel;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public List<Integer> getPointIndexes() {
        return pointIndexes;
    }

    public BigDecimal getTriggerValue() {
        return triggerValue;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }
}
