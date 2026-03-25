package com.example.demo.ingest.service;

import com.example.demo.ingest.dto.DeviceArrayRawRequest;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.DeviceRawDataEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.repository.DeviceRawDataJdbcRepository;
import com.example.demo.persistence.repository.DeviceRepository;
import com.example.demo.persistence.repository.MonitorRepository;
import com.example.demo.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceRawDataIngestService {

    private final DeviceRepository deviceRepository;
    private final MonitorRepository monitorRepository;
    private final DeviceRawDataJdbcRepository deviceRawDataJdbcRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public DeviceRawDataIngestService(
        DeviceRepository deviceRepository,
        MonitorRepository monitorRepository,
        DeviceRawDataJdbcRepository deviceRawDataJdbcRepository,
        IdGenerator idGenerator,
        ObjectMapper objectMapper
    ) {
        this.deviceRepository = deviceRepository;
        this.monitorRepository = monitorRepository;
        this.deviceRawDataJdbcRepository = deviceRawDataJdbcRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DeviceRawIngestResult ingest(DeviceArrayRawRequest request) {
        String iotCode = resolveIotCode(request);
        DeviceEntity device = deviceRepository.findActiveByIotCode(iotCode)
            .orElseThrow(() -> new IllegalArgumentException("Device not found for iotCode=" + iotCode));
        MonitorEntity monitor = monitorRepository.findActiveByDeviceId(device.getId()).orElse(null);
        LocalDateTime collectTime = request.getTimestamp() == null ? LocalDateTime.now() : request.getTimestamp();
        DeviceRawMetrics metrics = calculateMetrics(request.getValues());

        DeviceRawDataEntity entity = new DeviceRawDataEntity();
        entity.setId(idGenerator.nextId());
        entity.setDeviceId(device.getId());
        entity.setIotCode(device.getIotCode());
        entity.setMonitorId(monitor == null ? null : monitor.getId());
        entity.setTopic(request.getTopic());
        entity.setIedFullPath(request.getIedFullPath());
        entity.setCollectTime(collectTime);
        entity.setPointCount(request.getValues().size());
        entity.setValidStartPoint(request.getValidStartPoint());
        entity.setValidEndPoint(request.getValidEndPoint());
        entity.setValuesJson(toJson(buildSnapshot(request)));
        entity.setMaxTemp(metrics.getMaxTemp());
        entity.setMinTemp(metrics.getMinTemp());
        entity.setAvgTemp(metrics.getAvgTemp());
        entity.setDeleted(0);
        entity.setCreatedOn(LocalDateTime.now());
        deviceRawDataJdbcRepository.insert(entity);

        return new DeviceRawIngestResult(entity.getId(), device.getId(), entity.getIotCode(), entity.getMonitorId(), collectTime, metrics);
    }

    private String resolveIotCode(DeviceArrayRawRequest request) {
        if (request.getIotCode() != null && !request.getIotCode().trim().isEmpty()) {
            return request.getIotCode().trim();
        }
        if (request.getIedFullPath() != null && !request.getIedFullPath().trim().isEmpty()) {
            String value = request.getIedFullPath().trim();
            int index = value.lastIndexOf('/');
            return index >= 0 ? value.substring(index + 1) : value;
        }
        if (request.getTopic() != null && !request.getTopic().trim().isEmpty()) {
            String value = request.getTopic().trim();
            int index = value.lastIndexOf('/');
            return index >= 0 ? value.substring(index + 1) : value;
        }
        throw new IllegalArgumentException("iotCode or iedFullPath is required");
    }

    private DeviceRawMetrics calculateMetrics(List<Double> values) {
        BigDecimal max = null;
        BigDecimal min = null;
        BigDecimal total = BigDecimal.ZERO;
        for (Double value : values) {
            BigDecimal current = BigDecimal.valueOf(value.doubleValue());
            max = max == null || current.compareTo(max) > 0 ? current : max;
            min = min == null || current.compareTo(min) < 0 ? current : min;
            total = total.add(current);
        }
        BigDecimal avg = total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        return new DeviceRawMetrics(max, min, avg);
    }

    private Map<String, Object> buildSnapshot(DeviceArrayRawRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("topic", request.getTopic());
        payload.put("iedFullPath", request.getIedFullPath());
        payload.put("iotCode", request.getIotCode());
        payload.put("values", request.getValues());
        payload.put("validStartPoint", request.getValidStartPoint());
        payload.put("validEndPoint", request.getValidEndPoint());
        payload.put("timestamp", request.getTimestamp());
        return payload;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize device raw payload", ex);
        }
    }

    public static class DeviceRawIngestResult {
        private final Long id;
        private final Long deviceId;
        private final String iotCode;
        private final Long monitorId;
        private final LocalDateTime collectTime;
        private final DeviceRawMetrics metrics;

        public DeviceRawIngestResult(
            Long id,
            Long deviceId,
            String iotCode,
            Long monitorId,
            LocalDateTime collectTime,
            DeviceRawMetrics metrics
        ) {
            this.id = id;
            this.deviceId = deviceId;
            this.iotCode = iotCode;
            this.monitorId = monitorId;
            this.collectTime = collectTime;
            this.metrics = metrics;
        }

        public Long getId() {
            return id;
        }

        public Long getDeviceId() {
            return deviceId;
        }

        public String getIotCode() {
            return iotCode;
        }

        public Long getMonitorId() {
            return monitorId;
        }

        public LocalDateTime getCollectTime() {
            return collectTime;
        }

        public DeviceRawMetrics getMetrics() {
            return metrics;
        }
    }

    public static class DeviceRawMetrics {
        private final BigDecimal maxTemp;
        private final BigDecimal minTemp;
        private final BigDecimal avgTemp;

        public DeviceRawMetrics(BigDecimal maxTemp, BigDecimal minTemp, BigDecimal avgTemp) {
            this.maxTemp = maxTemp;
            this.minTemp = minTemp;
            this.avgTemp = avgTemp;
        }

        public BigDecimal getMaxTemp() {
            return maxTemp;
        }

        public BigDecimal getMinTemp() {
            return minTemp;
        }

        public BigDecimal getAvgTemp() {
            return avgTemp;
        }
    }
}
