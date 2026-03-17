package com.example.demo.realtime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {

    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

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

    public String minuteStatKey(String partitionCode, LocalDateTime statTime) {
        return "minute:stat:" + partitionCode + ":" + MINUTE_FORMATTER.format(statTime);
    }

    public String minuteStatPendingSetKey() {
        return "minute:stat:pending";
    }

    public String lastEventTimeKey(String alarmType, String partitionCode) {
        String scope = partitionCode == null || partitionCode.trim().isEmpty() ? "device" : partitionCode;
        return "alarm:last_event:" + alarmType + ":" + scope;
    }
}
