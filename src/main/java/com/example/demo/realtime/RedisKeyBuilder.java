package com.example.demo.realtime;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {

    public String lastReportKey(Long deviceId) {
        return "device:last_report:" + deviceId;
    }

    public String lastDataKey(Long deviceId) {
        return "device:last_data:" + deviceId;
    }

    public String partitionMeasureKey(String partitionCode) {
        return "partition:last_measure:" + partitionCode;
    }

    public String partitionWindowKey(String partitionCode) {
        return "partition:window:" + partitionCode;
    }

    public String partitionAlarmStateKey(String partitionCode) {
        return "partition:last_alarm:" + partitionCode;
    }

    public String activeAlarmKey(String alarmType, String partitionCode) {
        String scope = partitionCode == null || partitionCode.trim().isEmpty() ? "device" : partitionCode;
        return "alarm:active:" + alarmType + ":" + scope;
    }

    public String offlineLevelKey(Long deviceId) {
        return "device:offline_level:" + deviceId;
    }

    public String lastEventTimeKey(String alarmType, String partitionCode) {
        String scope = partitionCode == null || partitionCode.trim().isEmpty() ? "device" : partitionCode;
        return "alarm:last_event:" + alarmType + ":" + scope;
    }
}
