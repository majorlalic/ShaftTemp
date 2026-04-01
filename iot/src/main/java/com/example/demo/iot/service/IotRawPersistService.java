package com.example.demo.iot.service;

import com.example.demo.vo.PartitionAlarmRequest;
import com.example.demo.vo.PartitionMeasureRequest;
import com.example.demo.service.DeviceResolverService;
import com.example.demo.entity.AlarmRawEntity;
import com.example.demo.entity.DeviceEntity;
import com.example.demo.entity.RawDataEntity;
import com.example.demo.dao.AlarmRawRepository;
import com.example.demo.dao.RawDataRepository;
import com.example.demo.service.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IotRawPersistService {

    private final DeviceResolverService deviceResolverService;
    private final RawDataRepository rawDataRepository;
    private final AlarmRawRepository alarmRawRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public IotRawPersistService(
        DeviceResolverService deviceResolverService,
        RawDataRepository rawDataRepository,
        AlarmRawRepository alarmRawRepository,
        IdGenerator idGenerator,
        ObjectMapper objectMapper
    ) {
        this.deviceResolverService = deviceResolverService;
        this.rawDataRepository = rawDataRepository;
        this.alarmRawRepository = alarmRawRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public void persistMeasure(PartitionMeasureRequest request) {
        String partitionCode = extractPartitionCode(request.getDataReference());
        DeviceResolverService.ResolvedTarget resolved = deviceResolverService.resolve(
            request.getIotCode(),
            request.getPartitionId(),
            request.getDataReference(),
            partitionCode
        );
        DeviceEntity device = resolved.getDevice();
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
        rawData.setCollectTime(request.getTimestamp() == null ? LocalDateTime.now() : request.getTimestamp());
        rawData.setMaxTemp(request.getMaxTemp());
        rawData.setMinTemp(request.getMinTemp());
        rawData.setAvgTemp(request.getAvgTemp());
        rawData.setMaxTempPosition(request.getMaxTempPosition());
        rawData.setMinTempPosition(request.getMinTempPosition());
        rawData.setMaxTempChannel(request.getMaxTempChannel());
        rawData.setMinTempChannel(request.getMinTempChannel());
        rawData.setPayloadJson(toJson(buildMeasurePayload(request)));
        rawData.setDeleted(0);
        rawData.setCreatedOn(LocalDateTime.now());
        rawDataRepository.insert(rawData);
    }

    public void persistAlarmRaw(PartitionAlarmRequest request) {
        AlarmRawEntity entity = new AlarmRawEntity();
        entity.setId(idGenerator.nextId());
        entity.setIotCode(request.getIotCode());
        entity.setTopic(request.getTopic());
        entity.setPartitionId(request.getPartitionId());
        entity.setAlarmStatus(Boolean.TRUE.equals(request.getAlarmStatus()) ? 1 : 0);
        entity.setFaultStatus(Boolean.TRUE.equals(request.getFaultStatus()) ? 1 : 0);
        entity.setIedFullPath(request.getIedFullPath());
        entity.setDataReference(request.getDataReference());
        entity.setCollectTime(request.getTimestamp() == null ? LocalDateTime.now() : request.getTimestamp());
        entity.setPayloadJson(toJson(buildAlarmPayload(request)));
        entity.setDeleted(0);
        entity.setCreatedOn(LocalDateTime.now());
        alarmRawRepository.insert(entity);
    }

    private String extractPartitionCode(String dataReference) {
        if (dataReference == null || dataReference.trim().isEmpty()) {
            throw new IllegalArgumentException("dataReference is required");
        }
        int index = dataReference.lastIndexOf('/');
        return index >= 0 ? dataReference.substring(index + 1) : dataReference;
    }

    private Map<String, Object> buildMeasurePayload(PartitionMeasureRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
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

    private Map<String, Object> buildAlarmPayload(PartitionAlarmRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("topic", request.getTopic());
        payload.put("iotCode", request.getIotCode());
        payload.put("partitionId", request.getPartitionId());
        payload.put("iedFullPath", request.getIedFullPath());
        payload.put("dataReference", request.getDataReference());
        payload.put("alarmStatus", request.getAlarmStatus());
        payload.put("faultStatus", request.getFaultStatus());
        payload.put("timestamp", request.getTimestamp());
        return payload;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize iot payload", ex);
        }
    }
}

