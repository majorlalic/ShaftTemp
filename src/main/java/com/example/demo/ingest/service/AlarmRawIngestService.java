package com.example.demo.ingest.service;

import com.example.demo.ingest.dto.PartitionAlarmRequest;
import com.example.demo.persistence.entity.AlarmRawEntity;
import com.example.demo.persistence.repository.AlarmRawJdbcRepository;
import com.example.demo.support.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AlarmRawIngestService {

    private final AlarmRawJdbcRepository alarmRawJdbcRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public AlarmRawIngestService(
        AlarmRawJdbcRepository alarmRawJdbcRepository,
        IdGenerator idGenerator,
        ObjectMapper objectMapper
    ) {
        this.alarmRawJdbcRepository = alarmRawJdbcRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public Long ingest(PartitionAlarmRequest request) {
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
        entity.setPayloadJson(toJson(buildPayload(request)));
        entity.setDeleted(0);
        entity.setCreatedOn(LocalDateTime.now());
        alarmRawJdbcRepository.insert(entity);
        return entity.getId();
    }

    private Map<String, Object> buildPayload(PartitionAlarmRequest request) {
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
            throw new IllegalStateException("Failed to serialize alarm raw payload", ex);
        }
    }
}
