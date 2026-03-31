package com.example.demo.realtime;

import com.example.demo.AppProperties;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.ingest.dto.PartitionMeasureRequest;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.ingest.service.ReportIngestService.ReportMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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

    public void updateMeasureState(
        DeviceResolverService.ResolvedTarget resolved,
        LocalDateTime collectTime,
        PartitionMeasureRequest request,
        ReportMetrics metrics
    ) {
        Map<String, Object> summary = buildMeasureSummary(resolved, collectTime, request, metrics);
        redisTemplate.opsForValue().set(keyBuilder.lastReportKey(resolved.getDevice().getId()), FORMATTER.format(collectTime));
        redisTemplate.opsForValue().set(keyBuilder.lastDataKey(resolved.getDevice().getId()), toJson(summary));
        redisTemplate.opsForValue().set(keyBuilder.partitionMeasureKey(resolved.getPartitionCode()), toJson(summary));
        redisTemplate.opsForList().leftPush(keyBuilder.partitionWindowKey(resolved.getPartitionCode()), toJson(summary));
        redisTemplate.opsForList().trim(
            keyBuilder.partitionWindowKey(resolved.getPartitionCode()),
            0,
            appProperties.getAlarm().getWindowSize() - 1L
        );
        redisTemplate.opsForValue().set(keyBuilder.offlineLevelKey(resolved.getDevice().getId()), "0");
    }

    public void updateAlarmState(
        DeviceResolverService.ResolvedTarget resolved,
        LocalDateTime collectTime,
        boolean alarmStatus,
        boolean faultStatus
    ) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("deviceId", resolved.getDevice().getId());
        payload.put("monitorId", resolved.getMonitor().getId());
        payload.put("shaftFloorId", resolved.getShaftFloorId());
        payload.put("partitionCode", resolved.getPartitionCode());
        payload.put("partitionName", resolved.getPartitionName());
        payload.put("dataReference", resolved.getDataReference());
        payload.put("collectTime", FORMATTER.format(collectTime));
        payload.put("alarmStatus", alarmStatus);
        payload.put("faultStatus", faultStatus);
        redisTemplate.opsForValue().set(keyBuilder.lastReportKey(resolved.getDevice().getId()), FORMATTER.format(collectTime));
        redisTemplate.opsForValue().set(keyBuilder.partitionAlarmStateKey(resolved.getPartitionCode()), toJson(payload));
        redisTemplate.opsForValue().set(keyBuilder.offlineLevelKey(resolved.getDevice().getId()), "0");
    }

    public Optional<LocalDateTime> getLastReportTime(Long deviceId) {
        String value = redisTemplate.opsForValue().get(keyBuilder.lastReportKey(deviceId));
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(LocalDateTime.parse(value, FORMATTER));
    }

    public Optional<RealtimeSummary> getLastSummary(Long deviceId) {
        String value = redisTemplate.opsForValue().get(keyBuilder.lastDataKey(deviceId));
        return parseSummary(value);
    }

    public Optional<RealtimeSummary> getLastPartitionSummary(String partitionCode) {
        String value = redisTemplate.opsForValue().get(keyBuilder.partitionMeasureKey(partitionCode));
        return parseSummary(value);
    }

    private Optional<RealtimeSummary> parseSummary(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            return Optional.of(RealtimeSummary.from(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse realtime summary", ex);
        }
    }

    public Optional<Long> getActiveAlarmId(String alarmType, String scope) {
        String value = redisTemplate.opsForValue().get(keyBuilder.activeAlarmKey(alarmType, scope));
        return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
    }

    public void setActiveAlarmId(String alarmType, String scope, Long alarmId) {
        redisTemplate.opsForValue().set(keyBuilder.activeAlarmKey(alarmType, scope), String.valueOf(alarmId));
    }

    public void clearActiveAlarmId(String alarmType, String scope) {
        redisTemplate.delete(keyBuilder.activeAlarmKey(alarmType, scope));
    }

    public int incrementOfflineLevel(Long deviceId) {
        Long value = redisTemplate.opsForValue().increment(keyBuilder.offlineLevelKey(deviceId));
        return value == null ? 0 : value.intValue();
    }

    public void resetOfflineLevel(Long deviceId) {
        redisTemplate.opsForValue().set(keyBuilder.offlineLevelKey(deviceId), "0");
    }

    public boolean shouldWriteMergedEvent(String alarmType, String partitionCode, LocalDateTime eventTime) {
        long throttleSeconds = appProperties.getAlarm().getEventThrottleSeconds();
        if (throttleSeconds <= 0L) {
            return true;
        }
        String key = keyBuilder.lastEventTimeKey(alarmType, partitionCode);
        String lastEventTimeValue = redisTemplate.opsForValue().get(key);
        if (lastEventTimeValue == null || lastEventTimeValue.trim().isEmpty()) {
            return true;
        }
        LocalDateTime lastEventTime = LocalDateTime.parse(lastEventTimeValue, FORMATTER);
        return java.time.Duration.between(lastEventTime, eventTime).getSeconds() >= throttleSeconds;
    }

    public void markEventWritten(String alarmType, String partitionCode, LocalDateTime eventTime) {
        redisTemplate.opsForValue().set(keyBuilder.lastEventTimeKey(alarmType, partitionCode), FORMATTER.format(eventTime));
    }

    public Map<String, Object> buildAlarmDetail(
        DeviceResolverService.ResolvedTarget resolved,
        LocalDateTime eventTime,
        ReportMetrics metrics,
        RuleEvaluationResult result
    ) {
        Map<String, Object> detail = new HashMap<String, Object>();
        detail.put("deviceId", resolved.getDevice().getId());
        detail.put("monitorId", resolved.getMonitor().getId());
        detail.put("shaftFloorId", resolved.getShaftFloorId());
        detail.put("partitionCode", resolved.getPartitionCode());
        detail.put("partitionName", resolved.getPartitionName());
        detail.put("dataReference", resolved.getDataReference());
        detail.put("eventTime", FORMATTER.format(eventTime));
        if (metrics != null) {
            detail.put("maxTemp", metrics.getMaxTemp());
            detail.put("minTemp", metrics.getMinTemp());
            detail.put("avgTemp", metrics.getAvgTemp());
        }
        detail.put("triggerValue", result.getTriggerValue());
        detail.put("threshold", result.getThreshold());
        return detail;
    }

    private Map<String, Object> buildMeasureSummary(
        DeviceResolverService.ResolvedTarget resolved,
        LocalDateTime collectTime,
        PartitionMeasureRequest request,
        ReportMetrics metrics
    ) {
        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("deviceId", resolved.getDevice().getId());
        summary.put("monitorId", resolved.getMonitor().getId());
        summary.put("shaftFloorId", resolved.getShaftFloorId());
        summary.put("partitionCode", resolved.getPartitionCode());
        summary.put("partitionName", resolved.getPartitionName());
        summary.put("dataReference", resolved.getDataReference());
        summary.put("collectTime", FORMATTER.format(collectTime));
        summary.put("maxTemp", metrics.getMaxTemp());
        summary.put("minTemp", metrics.getMinTemp());
        summary.put("avgTemp", metrics.getAvgTemp());
        summary.put("maxTempPosition", request.getMaxTempPosition());
        summary.put("minTempPosition", request.getMinTempPosition());
        summary.put("maxTempChannel", request.getMaxTempChannel());
        summary.put("minTempChannel", request.getMinTempChannel());
        return summary;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize payload", ex);
        }
    }

    public static class RealtimeSummary {
        private Long deviceId;
        private Long monitorId;
        private Long shaftFloorId;
        private String partitionCode;
        private String partitionName;
        private String dataReference;
        private LocalDateTime collectTime;
        private BigDecimal maxTemp;
        private BigDecimal minTemp;
        private BigDecimal avgTemp;
        private BigDecimal maxTempPosition;
        private BigDecimal minTempPosition;
        private Integer maxTempChannel;
        private Integer minTempChannel;

        public static RealtimeSummary from(Map<String, Object> payload) {
            RealtimeSummary summary = new RealtimeSummary();
            Object deviceIdValue = payload.get("deviceId");
            if (deviceIdValue != null) {
                summary.setDeviceId(Long.valueOf(String.valueOf(deviceIdValue)));
            }
            Object monitorIdValue = payload.get("monitorId");
            if (monitorIdValue != null) {
                summary.setMonitorId(Long.valueOf(String.valueOf(monitorIdValue)));
            }
            Object shaftFloorIdValue = payload.get("shaftFloorId");
            if (shaftFloorIdValue != null && !"null".equals(String.valueOf(shaftFloorIdValue))) {
                summary.setShaftFloorId(Long.valueOf(String.valueOf(shaftFloorIdValue)));
            }
            Object partitionCodeValue = payload.get("partitionCode");
            if (partitionCodeValue != null) {
                summary.setPartitionCode(String.valueOf(partitionCodeValue));
            }
            Object partitionNameValue = payload.get("partitionName");
            if (partitionNameValue != null) {
                summary.setPartitionName(String.valueOf(partitionNameValue));
            }
            Object dataReferenceValue = payload.get("dataReference");
            if (dataReferenceValue != null) {
                summary.setDataReference(String.valueOf(dataReferenceValue));
            }
            Object collectTimeValue = payload.get("collectTime");
            if (collectTimeValue != null) {
                summary.setCollectTime(LocalDateTime.parse(String.valueOf(collectTimeValue), FORMATTER));
            }
            Object maxTempValue = payload.get("maxTemp");
            if (maxTempValue != null) {
                summary.setMaxTemp(new BigDecimal(String.valueOf(maxTempValue)));
            }
            Object minTempValue = payload.get("minTemp");
            if (minTempValue != null) {
                summary.setMinTemp(new BigDecimal(String.valueOf(minTempValue)));
            }
            Object avgTempValue = payload.get("avgTemp");
            if (avgTempValue != null) {
                summary.setAvgTemp(new BigDecimal(String.valueOf(avgTempValue)));
            }
            Object maxTempPositionValue = payload.get("maxTempPosition");
            if (maxTempPositionValue != null && !"null".equals(String.valueOf(maxTempPositionValue))) {
                summary.setMaxTempPosition(new BigDecimal(String.valueOf(maxTempPositionValue)));
            }
            Object minTempPositionValue = payload.get("minTempPosition");
            if (minTempPositionValue != null && !"null".equals(String.valueOf(minTempPositionValue))) {
                summary.setMinTempPosition(new BigDecimal(String.valueOf(minTempPositionValue)));
            }
            Object maxTempChannelValue = payload.get("maxTempChannel");
            if (maxTempChannelValue != null && !"null".equals(String.valueOf(maxTempChannelValue))) {
                summary.setMaxTempChannel(Integer.valueOf(String.valueOf(maxTempChannelValue)));
            }
            Object minTempChannelValue = payload.get("minTempChannel");
            if (minTempChannelValue != null && !"null".equals(String.valueOf(minTempChannelValue))) {
                summary.setMinTempChannel(Integer.valueOf(String.valueOf(minTempChannelValue)));
            }
            return summary;
        }

        public Long getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(Long deviceId) {
            this.deviceId = deviceId;
        }

        public Long getMonitorId() {
            return monitorId;
        }

        public void setMonitorId(Long monitorId) {
            this.monitorId = monitorId;
        }

        public Long getShaftFloorId() {
            return shaftFloorId;
        }

        public void setShaftFloorId(Long shaftFloorId) {
            this.shaftFloorId = shaftFloorId;
        }

        public String getPartitionCode() {
            return partitionCode;
        }

        public void setPartitionCode(String partitionCode) {
            this.partitionCode = partitionCode;
        }

        public String getPartitionName() {
            return partitionName;
        }

        public void setPartitionName(String partitionName) {
            this.partitionName = partitionName;
        }

        public String getDataReference() {
            return dataReference;
        }

        public void setDataReference(String dataReference) {
            this.dataReference = dataReference;
        }

        public LocalDateTime getCollectTime() {
            return collectTime;
        }

        public void setCollectTime(LocalDateTime collectTime) {
            this.collectTime = collectTime;
        }

        public BigDecimal getMaxTemp() {
            return maxTemp;
        }

        public void setMaxTemp(BigDecimal maxTemp) {
            this.maxTemp = maxTemp;
        }

        public BigDecimal getMinTemp() {
            return minTemp;
        }

        public void setMinTemp(BigDecimal minTemp) {
            this.minTemp = minTemp;
        }

        public BigDecimal getAvgTemp() {
            return avgTemp;
        }

        public void setAvgTemp(BigDecimal avgTemp) {
            this.avgTemp = avgTemp;
        }

        public BigDecimal getMaxTempPosition() {
            return maxTempPosition;
        }

        public void setMaxTempPosition(BigDecimal maxTempPosition) {
            this.maxTempPosition = maxTempPosition;
        }

        public BigDecimal getMinTempPosition() {
            return minTempPosition;
        }

        public void setMinTempPosition(BigDecimal minTempPosition) {
            this.minTempPosition = minTempPosition;
        }

        public Integer getMaxTempChannel() {
            return maxTempChannel;
        }

        public void setMaxTempChannel(Integer maxTempChannel) {
            this.maxTempChannel = maxTempChannel;
        }

        public Integer getMinTempChannel() {
            return minTempChannel;
        }

        public void setMinTempChannel(Integer minTempChannel) {
            this.minTempChannel = minTempChannel;
        }
    }

}
