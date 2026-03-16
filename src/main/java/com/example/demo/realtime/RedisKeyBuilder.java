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

    public String windowKey(Long deviceId) {
        return "device:window:" + deviceId;
    }

    public String activeAlarmKey(String alarmType, Long monitorId) {
        return "alarm:active:" + alarmType + ":" + monitorId;
    }

    public String offlineLevelKey(Long deviceId) {
        return "device:offline_level:" + deviceId;
    }
}
