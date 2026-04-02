package com.example.demo.service;

import com.example.demo.vo.PartitionAlarmRequest;
import com.csg.dgri.szsiom.sysmanage.model.AlarmRawVO;
import com.csg.dgri.szsiom.sysmanage.appservice.AlarmRawAppService;
import com.example.demo.service.IdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AlarmRawIngestService {

    private final AlarmRawAppService<?> alarmRawRepository;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public AlarmRawIngestService(
        AlarmRawAppService<?> alarmRawRepository,
        IdGenerator idGenerator,
        ObjectMapper objectMapper
    ) {
        this.alarmRawRepository = alarmRawRepository;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    public Long ingest(PartitionAlarmRequest request) {
        AlarmRawVO entity = new AlarmRawVO();
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
        alarmRawRepository.insert(entity);
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
