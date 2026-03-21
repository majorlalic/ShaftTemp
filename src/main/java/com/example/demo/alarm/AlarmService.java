package com.example.demo.alarm;

import com.example.demo.alarm.rule.RuleEvaluationResult;
import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.persistence.entity.AlarmEntity;
import java.time.LocalDateTime;

public interface AlarmService {

    AlarmEntity createOrMerge(
        DeviceResolverService.ResolvedTarget resolved,
        RuleEvaluationResult result,
        LocalDateTime eventTime,
        String detailJson,
        String pointListJson
    );

    void recover(
        DeviceResolverService.ResolvedTarget resolved,
        String alarmType,
        LocalDateTime eventTime,
        String detailJson
    );

    AlarmEntity confirm(Long alarmId, Long handler, String remark);

    AlarmEntity observe(Long alarmId, String remark);

    AlarmEntity markFalsePositive(Long alarmId, String remark);

    AlarmEntity close(Long alarmId, String remark);
}
