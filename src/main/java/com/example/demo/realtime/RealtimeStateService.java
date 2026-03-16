package com.example.demo.realtime;

import com.example.demo.AppProperties;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.ingest.service.ReportIngestService.ReportMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeStateService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyBuilder keyBuilder;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public RealtimeStateService(
        StringRedisTemplate redisTemplate,
        RedisKeyBuilder keyBuilder,
        ObjectMapper objectMapper,
        AppProperties appProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.keyBuilder = keyBuilder;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    public void updateRealtimeState(
        Long deviceId,
        Long monitorId,
        LocalDateTime collectTime,
        List<?> values,
        ReportMetrics metrics
    ) {
        redisTemplate.opsForValue().set(keyBuilder.lastReportKey(deviceId), FORMATTER.format(collectTime));
        redisTemplate.opsForValue().set(keyBuilder.lastDataKey(deviceId), toJson(buildSummary(monitorId, collectTime, values, metrics)));
        redisTemplate.opsForList().leftPush(keyBuilder.windowKey(deviceId), toJson(buildSummary(monitorId, collectTime, values, metrics)));
        redisTemplate.opsForList().trim(keyBuilder.windowKey(deviceId), 0, appProperties.getAlarm().getWindowSize() - 1L);
        redisTemplate.opsForValue().set(keyBuilder.offlineLevelKey(deviceId), "0");
    }

    public Optional<LocalDateTime> getLastReportTime(Long deviceId) {
        String value = redisTemplate.opsForValue().get(keyBuilder.lastReportKey(deviceId));
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(LocalDateTime.parse(value, FORMATTER));
    }

    public Optional<Long> getActiveAlarmId(String alarmType, Long monitorId) {
        String value = redisTemplate.opsForValue().get(keyBuilder.activeAlarmKey(alarmType, monitorId));
        return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
    }

    public void setActiveAlarmId(String alarmType, Long monitorId, Long alarmId) {
        redisTemplate.opsForValue().set(keyBuilder.activeAlarmKey(alarmType, monitorId), String.valueOf(alarmId));
    }

    public int incrementOfflineLevel(Long deviceId) {
        Long value = redisTemplate.opsForValue().increment(keyBuilder.offlineLevelKey(deviceId));
        return value == null ? 0 : value.intValue();
    }

    public void resetOfflineLevel(Long deviceId) {
        redisTemplate.opsForValue().set(keyBuilder.offlineLevelKey(deviceId), "0");
    }

    public Map<String, Object> buildAlarmDetail(
        Long deviceId,
        Long monitorId,
        LocalDateTime eventTime,
        ReportMetrics metrics,
        RuleEvaluationResult result
    ) {
        Map<String, Object> detail = new HashMap<String, Object>();
        detail.put("deviceId", deviceId);
        detail.put("monitorId", monitorId);
        detail.put("eventTime", FORMATTER.format(eventTime));
        detail.put("maxTemp", metrics.getMaxTemp());
        detail.put("minTemp", metrics.getMinTemp());
        detail.put("avgTemp", metrics.getAvgTemp());
        detail.put("triggerValue", result.getTriggerValue());
        detail.put("threshold", result.getThreshold());
        return detail;
    }

    private Map<String, Object> buildSummary(Long monitorId, LocalDateTime collectTime, List<?> values, ReportMetrics metrics) {
        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("monitorId", monitorId);
        summary.put("collectTime", FORMATTER.format(collectTime));
        summary.put("values", values);
        summary.put("maxTemp", metrics.getMaxTemp());
        summary.put("minTemp", metrics.getMinTemp());
        summary.put("avgTemp", metrics.getAvgTemp());
        return summary;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize payload", ex);
        }
    }
}
