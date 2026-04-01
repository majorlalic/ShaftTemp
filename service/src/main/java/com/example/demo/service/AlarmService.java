package com.example.demo.service;

import com.example.demo.service.RuleEvaluationResult;
import com.example.demo.service.DeviceResolverService;
import com.example.demo.entity.AlarmEntity;
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
