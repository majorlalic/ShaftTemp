package com.example.demo.ingest.service;

import com.example.demo.alarm.AlarmService;
import com.example.demo.alarm.rule.AlarmRuleEngine;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.ingest.dto.TemperatureReportRequest;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.DeviceOnlineLogEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.entity.RawDataEntity;
import com.example.demo.persistence.repository.DeviceOnlineLogRepository;
import com.example.demo.persistence.repository.DeviceRepository;
import com.example.demo.persistence.repository.RawDataRepository;
import com.example.demo.persistence.service.TempStatMinuteService;
import com.example.demo.realtime.RealtimeStateService;
import com.example.demo.realtime.RealtimeStateService.RealtimeSummary;
import com.example.demo.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportIngestService {

    private final DeviceResolverService deviceResolverService;
    private final DeviceRepository deviceRepository;
    private final RawDataRepository rawDataRepository;
    private final DeviceOnlineLogRepository deviceOnlineLogRepository;
    private final RealtimeStateService realtimeStateService;
    private final AlarmRuleEngine alarmRuleEngine;
    private final AlarmService alarmService;
    private final TempStatMinuteService tempStatMinuteService;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public ReportIngestService(
        DeviceResolverService deviceResolverService,
        DeviceRepository deviceRepository,
        RawDataRepository rawDataRepository,
        DeviceOnlineLogRepository deviceOnlineLogRepository,
        RealtimeStateService realtimeStateService,
        AlarmRuleEngine alarmRuleEngine,
        AlarmService alarmService,
        TempStatMinuteService tempStatMinuteService,
        IdGenerator idGenerator,
        ObjectMapper objectMapper
    ) {
        this.deviceResolverService = deviceResolverService;
        this.deviceRepository = deviceRepository;
        this.rawDataRepository = rawDataRepository;
        this.deviceOnlineLogRepository = deviceOnlineLogRepository;
        this.realtimeStateService = realtimeStateService;
        this.alarmRuleEngine = alarmRuleEngine;
        this.alarmService = alarmService;
        this.tempStatMinuteService = tempStatMinuteService;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestResult ingest(TemperatureReportRequest request) {
        DeviceResolverService.ResolvedDevice resolved = deviceResolverService.resolve(request.getIotCode());
        DeviceEntity device = resolved.getDevice();
        MonitorEntity monitor = resolved.getMonitor();
        LocalDateTime collectTime = request.getCollectTime() == null ? LocalDateTime.now() : request.getCollectTime();
        RealtimeSummary previousSummary = realtimeStateService.getLastSummary(device.getId()).orElse(null);
        ReportMetrics metrics = calculateMetrics(request.getValues());
        List<RuleEvaluationResult> results = alarmRuleEngine.evaluateRealtime(monitor, request.getValues(), previousSummary);

        RawDataEntity rawData = new RawDataEntity();
        rawData.setId(idGenerator.nextId());
        rawData.setDeviceId(device.getId());
        rawData.setIotCode(device.getIotCode());
        rawData.setMonitorId(monitor.getId());
        rawData.setCollectTime(collectTime);
        rawData.setPointCount(request.getValues().size());
        rawData.setValidStartPoint(1);
        rawData.setValidEndPoint(request.getValues().size());
        rawData.setValuesJson(toJson(request.getValues()));
        rawData.setMaxTemp(metrics.getMaxTemp());
        rawData.setMinTemp(metrics.getMinTemp());
        rawData.setAvgTemp(metrics.getAvgTemp());
        rawData.setAbnormalFlag(results.isEmpty() ? 0 : 1);
        rawData.setDeleted(0);
        rawData.setCreatedOn(LocalDateTime.now());
        rawDataRepository.save(rawData);

        tempStatMinuteService.aggregate(device.getId(), monitor.getId(), collectTime, metrics, countAlarmPoints(results));
        realtimeStateService.updateRealtimeState(device.getId(), monitor.getId(), collectTime, request.getValues(), metrics);
        updateDeviceOnlineState(device, collectTime);

        for (RuleEvaluationResult result : results) {
            alarmService.createOrMerge(
                device,
                monitor,
                result,
                collectTime,
                toJson(realtimeStateService.buildAlarmDetail(device.getId(), monitor.getId(), collectTime, metrics, result)),
                toJson(result.getPointIndexes())
            );
        }
        recoverInactiveRealtimeAlarms(device, monitor, results, collectTime);

        return new IngestResult(device.getId(), monitor.getId(), rawData.getId(), results.size(), metrics);
    }

    private void recoverInactiveRealtimeAlarms(
        DeviceEntity device,
        MonitorEntity monitor,
        List<RuleEvaluationResult> results,
        LocalDateTime collectTime
    ) {
        String[] realtimeAlarmTypes = new String[] {"TEMP_THRESHOLD", "TEMP_DIFFERENCE", "TEMP_RISE_RATE", "FIBER_BREAK"};
        for (String alarmType : realtimeAlarmTypes) {
            if (!containsAlarmType(results, alarmType)) {
                alarmService.recover(
                    device,
                    monitor,
                    alarmType,
                    collectTime,
                    toJson(java.util.Collections.singletonMap("message", "alarm recovered by latest report"))
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

    private int countAlarmPoints(List<RuleEvaluationResult> results) {
        int total = 0;
        for (RuleEvaluationResult result : results) {
            total += result.getPointIndexes() == null ? 0 : result.getPointIndexes().size();
        }
        return total;
    }

    private void updateDeviceOnlineState(DeviceEntity device, LocalDateTime collectTime) {
        boolean wasOffline = device.getOnlineStatus() == null || device.getOnlineStatus() == 0;
        device.setOnlineStatus(1);
        device.setLastReportTime(collectTime);
        device.setUpdatedOn(LocalDateTime.now());
        deviceRepository.save(device);
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
            deviceOnlineLogRepository.save(log);
        }
    }

    public ReportMetrics calculateMetrics(List<BigDecimal> values) {
        BigDecimal maxTemp = values.get(0);
        BigDecimal minTemp = values.get(0);
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value.compareTo(maxTemp) > 0) {
                maxTemp = value;
            }
            if (value.compareTo(minTemp) < 0) {
                minTemp = value;
            }
            total = total.add(value);
        }
        BigDecimal avgTemp = total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        return new ReportMetrics(maxTemp, minTemp, avgTemp);
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
        private final Long rawDataId;
        private final int alarmCount;
        private final ReportMetrics metrics;

        public IngestResult(Long deviceId, Long monitorId, Long rawDataId, int alarmCount, ReportMetrics metrics) {
            this.deviceId = deviceId;
            this.monitorId = monitorId;
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
