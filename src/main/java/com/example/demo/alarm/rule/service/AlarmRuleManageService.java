package com.example.demo.alarm.rule.service;

import com.example.demo.persistence.entity.AlarmRuleEntity;
import com.example.demo.persistence.repository.AlarmRuleRepository;
import com.example.demo.support.IdGenerator;
import com.example.demo.alarm.rule.api.AlarmRuleUpsertRequest;
import com.example.demo.alarm.rule.service.AlarmRuleResolverService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmRuleManageService {

    private final AlarmRuleRepository alarmRuleRepository;
    private final AlarmRuleResolverService alarmRuleResolverService;
    private final IdGenerator idGenerator;

    public AlarmRuleManageService(
        AlarmRuleRepository alarmRuleRepository,
        AlarmRuleResolverService alarmRuleResolverService,
        IdGenerator idGenerator
    ) {
        this.alarmRuleRepository = alarmRuleRepository;
        this.alarmRuleResolverService = alarmRuleResolverService;
        this.idGenerator = idGenerator;
    }

    public List<Map<String, Object>> listRules(String bizType, String alarmType, String scopeType, Integer enabled) {
        return alarmRuleRepository.findAllActive().stream()
            .filter(rule -> bizType == null || bizType.equals(rule.getBizType()))
            .filter(rule -> alarmType == null || alarmType.equals(rule.getAlarmType()))
            .filter(rule -> scopeType == null || "GLOBAL".equals(rule.getScopeType()))
            .filter(rule -> enabled == null || enabled.equals(rule.getEnabled()))
            .map(this::toMap)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getRule(Long id) {
        return toMap(findRule(id));
    }

    @Transactional
    public Map<String, Object> createRule(AlarmRuleUpsertRequest request) {
        LocalDateTime now = LocalDateTime.now();
        AlarmRuleEntity entity = new AlarmRuleEntity();
        entity.setId(idGenerator.nextId());
        entity.setDeleted(0);
        entity.setCreatedOn(now);
        fill(entity, request, now);
        alarmRuleRepository.insert(entity);
        Map<String, Object> payload = toMap(entity);
        alarmRuleResolverService.refresh();
        return payload;
    }

    @Transactional
    public Map<String, Object> updateRule(Long id, AlarmRuleUpsertRequest request) {
        AlarmRuleEntity entity = findRule(id);
        fill(entity, request, LocalDateTime.now());
        alarmRuleRepository.updateById(entity);
        Map<String, Object> payload = toMap(entity);
        alarmRuleResolverService.refresh();
        return payload;
    }

    @Transactional
    public Map<String, Object> setEnabled(Long id, Integer enabled) {
        AlarmRuleEntity entity = findRule(id);
        entity.setEnabled(enabled);
        entity.setUpdatedOn(LocalDateTime.now());
        alarmRuleRepository.updateById(entity);
        Map<String, Object> payload = toMap(entity);
        alarmRuleResolverService.refresh();
        return payload;
    }

    @Transactional
    public Map<String, Object> deleteRule(Long id) {
        AlarmRuleEntity entity = findRule(id);
        entity.setDeleted(1);
        entity.setUpdatedOn(LocalDateTime.now());
        alarmRuleRepository.updateById(entity);
        Map<String, Object> payload = toMap(entity);
        alarmRuleResolverService.refresh();
        return payload;
    }

    private AlarmRuleEntity findRule(Long id) {
        return alarmRuleRepository.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Alarm rule not found: " + id));
    }

    private void fill(AlarmRuleEntity entity, AlarmRuleUpsertRequest request, LocalDateTime now) {
        entity.setRuleName(request.getRuleName());
        entity.setBizType(request.getBizType());
        entity.setAlarmType(request.getAlarmType());
        entity.setScopeType("GLOBAL");
        entity.setScopeId(null);
        entity.setLevel(request.getLevel());
        entity.setThresholdValue(request.getThresholdValue());
        entity.setThresholdValue2(request.getThresholdValue2());
        entity.setDurationSeconds(request.getDurationSeconds());
        entity.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        entity.setRemark(request.getRemark());
        entity.setUpdatedOn(now);
    }

    private Map<String, Object> toMap(AlarmRuleEntity entity) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("id", entity.getId());
        payload.put("ruleName", entity.getRuleName());
        payload.put("bizType", entity.getBizType());
        payload.put("alarmType", entity.getAlarmType());
        payload.put("scopeType", entity.getScopeType());
        payload.put("scopeId", entity.getScopeId());
        payload.put("level", entity.getLevel());
        payload.put("thresholdValue", entity.getThresholdValue());
        payload.put("thresholdValue2", entity.getThresholdValue2());
        payload.put("durationSeconds", entity.getDurationSeconds());
        payload.put("enabled", entity.getEnabled());
        payload.put("remark", entity.getRemark());
        payload.put("updatedOn", entity.getUpdatedOn());
        return payload;
    }
}
