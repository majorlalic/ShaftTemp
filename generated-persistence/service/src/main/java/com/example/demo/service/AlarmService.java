package com.example.demo.service;

import com.example.demo.service.RuleEvaluationResult;
import com.example.demo.service.DeviceResolverService;
import com.csg.dgri.szsiom.sysmanage.model.AlarmVO;
import java.time.LocalDateTime;

public interface AlarmService {

    AlarmVO createOrMerge(
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

    AlarmVO handle(Long alarmId, Integer status, String remark);

    AlarmVO confirm(Long alarmId, Long handler, String remark);

    AlarmVO observe(Long alarmId, String remark);

    AlarmVO markFalsePositive(Long alarmId, String remark);

    AlarmVO close(Long alarmId, String remark);
}
