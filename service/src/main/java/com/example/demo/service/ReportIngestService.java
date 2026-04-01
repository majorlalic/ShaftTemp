package com.example.demo.service;

import com.example.demo.service.AlarmService;
import com.example.demo.service.AlarmRuleEngine;
import com.example.demo.service.RuleEvaluationResult;
import com.example.demo.vo.PartitionAlarmRequest;
import com.example.demo.vo.PartitionMeasureRequest;
import com.example.demo.entity.DeviceEntity;
import com.example.demo.entity.DeviceOnlineLogEntity;
import com.example.demo.entity.RawDataEntity;
import com.example.demo.dao.DeviceOnlineLogRepository;
import com.example.demo.dao.DeviceRepository;
import com.example.demo.dao.RawDataRepository;
import com.example.demo.service.RealtimeStateService;
import com.example.demo.service.RealtimeStateService.RealtimeSummary;
import com.example.demo.service.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportIngestService {

    private final DeviceResolverService deviceResolverService;
    private final DeviceRepository deviceRepository;
    private final DeviceOnlineLogRepository deviceOnlineLogRepository;
    private final RealtimeStateService realtimeStateService;
    private final AlarmRuleEngine alarmRuleEngine;
    private final AlarmService alarmService;
    private final AlarmRawIngestService alarmRawIngestService;
    private final RawDataRepository rawDataRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public ReportIngestService(
        DeviceResolverService deviceResolverService,
        DeviceRepository deviceRepository,
        DeviceOnlineLogRepository deviceOnlineLogRepository,
        RealtimeStateService realtimeStateService,
        AlarmRuleEngine alarmRuleEngine,
        AlarmService alarmService,
        AlarmRawIngestService alarmRawIngestService,
        RawDataRepository rawDataRepository,
        IdGenerator idGenerator,
        ObjectMapper objectMapper
    ) {
        this.deviceResolverService = deviceResolverService;
        this.deviceRepository = deviceRepository;
        this.deviceOnlineLogRepository = deviceOnlineLogRepository;
        this.realtimeStateService = realtimeStateService;
        this.alarmRuleEngine = alarmRuleEngine;
        this.alarmService = alarmService;
        this.alarmRawIngestService = alarmRawIngestService;
        this.rawDataRepository = rawDataRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestResult ingestMeasure(PartitionMeasureRequest request) {
        return ingestMeasureInternal(request, true);
    }

    @Transactional
    public IngestResult processMeasureWithoutRawData(PartitionMeasureRequest request) {
        return ingestMeasureInternal(request, false);
    }

    private IngestResult ingestMeasureInternal(PartitionMeasureRequest request, boolean persistRawData) {
        String partitionCode = extractPartitionCode(request.getDataReference());
        DeviceResolverService.ResolvedTarget resolved = deviceResolverService.resolve(
            request.getIotCode(),
            request.getPartitionId(),
            request.getDataReference(),
            partitionCode
        );
        DeviceEntity device = resolved.getDevice();
        LocalDateTime collectTime = request.getTimestamp() == null ? LocalDateTime.now() : request.getTimestamp();
        RealtimeSummary previousSummary = realtimeStateService.getLastPartitionSummary(resolved.getPartitionCode()).orElse(null);
        ReportMetrics metrics = new ReportMetrics(request.getMaxTemp(), request.getMinTemp(), request.getAvgTemp());
        List<RuleEvaluationResult> results = alarmRuleEngine.evaluateMeasure(
            resolved,
            metrics,
            previousSummary
        );

        Long rawDataId = null;
        if (persistRawData) {
            RawDataEntity rawData = buildRawDataEntity(request, resolved, device, collectTime, metrics);
            rawDataRepository.insert(rawData);
            rawDataId = rawData.getId();
        }

        realtimeStateService.updateMeasureState(resolved, collectTime, request, metrics);
        updateDeviceOnlineState(device, collectTime);

        for (RuleEvaluationResult result : results) {
            alarmService.createOrMerge(
                resolved,
                result,
                collectTime,
                toJson(realtimeStateService.buildAlarmDetail(resolved, collectTime, metrics, result)),
                "[]"
            );
        }
        recoverInactiveMeasureAlarms(resolved, results, collectTime);

        return new IngestResult(device.getId(), resolved.getMonitor().getId(), resolved.getPartitionCode(), rawDataId, results.size(), metrics);
    }

    private RawDataEntity buildRawDataEntity(
        PartitionMeasureRequest request,
        DeviceResolverService.ResolvedTarget resolved,
        DeviceEntity device,
        LocalDateTime collectTime,
        ReportMetrics metrics
    ) {
        RawDataEntity rawData = new RawDataEntity();
        rawData.setId(idGenerator.nextId());
        rawData.setDeviceId(device.getId());
        rawData.setIotCode(request.getIotCode() == null ? device.getIotCode() : request.getIotCode());
        rawData.setTopic(request.getTopic());
        rawData.setPartitionId(request.getPartitionId() == null ? resolved.getPartitionId() : request.getPartitionId());
        rawData.setMonitorId(resolved.getMonitor().getId());
        rawData.setShaftFloorId(resolved.getShaftFloorId());
        rawData.setDataReference(resolved.getDataReference());
        rawData.setIedFullPath(request.getIedFullPath());
        rawData.setCollectTime(collectTime);
        rawData.setMaxTemp(metrics.getMaxTemp());
        rawData.setMinTemp(metrics.getMinTemp());
        rawData.setAvgTemp(metrics.getAvgTemp());
        rawData.setMaxTempPosition(request.getMaxTempPosition());
        rawData.setMinTempPosition(request.getMinTempPosition());
        rawData.setMaxTempChannel(request.getMaxTempChannel());
        rawData.setMinTempChannel(request.getMinTempChannel());
        rawData.setPayloadJson(toJson(buildMeasureSnapshot(request)));
        rawData.setDeleted(0);
        rawData.setCreatedOn(LocalDateTime.now());
        return rawData;
    }

    @Transactional
    public IngestResult ingestAlarm(PartitionAlarmRequest request) {
        Long alarmRawId = alarmRawIngestService.ingest(request);
        return new IngestResult(null, null, null, alarmRawId, 0, null);
    }

    private void recoverInactiveMeasureAlarms(
        DeviceResolverService.ResolvedTarget resolved,
        List<RuleEvaluationResult> results,
        LocalDateTime collectTime
    ) {
        String[] realtimeAlarmTypes = new String[] {"TEMP_THRESHOLD", "TEMP_DIFFERENCE", "TEMP_RISE_RATE"};
        for (String alarmType : realtimeAlarmTypes) {
            if (!containsAlarmType(results, alarmType)) {
                alarmService.recover(
                    resolved,
                    alarmType,
                    collectTime,
                    toJson(Collections.singletonMap("message", "alarm recovered by latest measure"))
                );
            }
        }
    }

    private boolean containsAlarmType(List<RuleEvaluationResult> results, String alarmType) {
        for (RuleEvaluationResult result : results) {
            if (alarmType.equals(result.getAlarmType())) {
                return true;
            }
        }
        return false;
    }

    private void updateDeviceOnlineState(DeviceEntity device, LocalDateTime collectTime) {
        boolean wasOffline = device.getOnlineStatus() == null || device.getOnlineStatus() == 0;
        device.setOnlineStatus(1);
        device.setLastReportTime(collectTime);
        device.setUpdatedOn(LocalDateTime.now());
        deviceRepository.updateById(device);
        realtimeStateService.resetOfflineLevel(device.getId());

        if (wasOffline) {
            DeviceOnlineLogEntity log = new DeviceOnlineLogEntity();
            log.setId(idGenerator.nextId());
            log.setDeviceId(device.getId());
            log.setStatus(1);
            log.setChangeTime(collectTime);
            log.setReason("received report");
            log.setDeleted(0);
            log.setCreatedOn(LocalDateTime.now());
            deviceOnlineLogRepository.insert(log);
        }
    }

    private String extractPartitionCode(String dataReference) {
        if (dataReference == null || dataReference.trim().isEmpty()) {
            throw new IllegalArgumentException("dataReference is required");
        }
        int index = dataReference.lastIndexOf('/');
        return index >= 0 ? dataReference.substring(index + 1) : dataReference;
    }

    private Object buildMeasureSnapshot(PartitionMeasureRequest request) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("topic", request.getTopic());
        payload.put("iotCode", request.getIotCode());
        payload.put("partitionId", request.getPartitionId());
        payload.put("iedFullPath", request.getIedFullPath());
        payload.put("dataReference", request.getDataReference());
        payload.put("maxTemp", request.getMaxTemp());
        payload.put("minTemp", request.getMinTemp());
        payload.put("avgTemp", request.getAvgTemp());
        payload.put("maxTempPosition", request.getMaxTempPosition());
        payload.put("minTempPosition", request.getMinTempPosition());
        payload.put("maxTempChannel", request.getMaxTempChannel());
        payload.put("minTempChannel", request.getMinTempChannel());
        payload.put("timestamp", request.getTimestamp());
        return payload;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize data", ex);
        }
    }

    public static class IngestResult {
        private final Long deviceId;
        private final Long monitorId;
        private final String partitionCode;
        private final Long rawDataId;
        private final int alarmCount;
        private final ReportMetrics metrics;

        public IngestResult(Long deviceId, Long monitorId, String partitionCode, Long rawDataId, int alarmCount, ReportMetrics metrics) {
            this.deviceId = deviceId;
            this.monitorId = monitorId;
            this.partitionCode = partitionCode;
            this.rawDataId = rawDataId;
            this.alarmCount = alarmCount;
            this.metrics = metrics;
        }

        public Long getDeviceId() {
            return deviceId;
        }

        public Long getMonitorId() {
            return monitorId;
        }

        public String getPartitionCode() {
            return partitionCode;
        }

        public Long getRawDataId() {
            return rawDataId;
        }

        public int getAlarmCount() {
            return alarmCount;
        }

        public ReportMetrics getMetrics() {
            return metrics;
        }
    }

    public static class ReportMetrics {
        private final BigDecimal maxTemp;
        private final BigDecimal minTemp;
        private final BigDecimal avgTemp;

        public ReportMetrics(BigDecimal maxTemp, BigDecimal minTemp, BigDecimal avgTemp) {
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
