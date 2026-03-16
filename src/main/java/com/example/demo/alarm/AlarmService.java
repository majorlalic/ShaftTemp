package com.example.demo.alarm;

import com.example.demo.persistence.entity.AlarmEntity;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.alarm.rule.RuleEvaluationResult;
import java.time.LocalDateTime;

public interface AlarmService {

    AlarmEntity createOrMerge(
        DeviceEntity device,
        MonitorEntity monitor,
        RuleEvaluationResult result,
        LocalDateTime eventTime,
        String detailJson,
        String pointListJson
    );
}
