package com.example.demo.service;

public final class AlarmDomain {

    public static final int TERMINAL = 0;
    public static final int MONITOR = 1;

    private AlarmDomain() {}

    public static Integer fromAlarmType(String alarmType) {
        if ("DEVICE_OFFLINE".equals(alarmType) || "PARTITION_FAULT".equals(alarmType)) {
            return Integer.valueOf(TERMINAL);
        }
        return Integer.valueOf(MONITOR);
    }
}

