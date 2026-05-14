package com.example.demo.service;

public final class AlarmStatus {

    public static final int PENDING_CONFIRM = 1;
    public static final int OBSERVING = 11;
    public static final int PENDING_RECTIFICATION = 12;
    public static final int PENDING_RETEST = 13;
    public static final int CONFIRMED = 3;
    public static final int CLOSED = 14;
    public static final int FALSE_POSITIVE = PENDING_RETEST;
    public static final int AUTO_RECOVERED = CLOSED;
    public static final String ACTIVE_MONITOR_STATUS_SQL = "(1,11,12,13,3)";
    public static final String CLOSED_STATUS_SQL = "14";

    private AlarmStatus() {}

    public static String nameOf(Integer code) {
        if (code == null) {
            return null;
        }
        switch (code.intValue()) {
            case PENDING_CONFIRM:
                return "待确认";
            case OBSERVING:
                return "持续观察";
            case PENDING_RECTIFICATION:
                return "待消缺";
            case PENDING_RETEST:
                return "待复测";
            case CONFIRMED:
                return "已确认";
            case CLOSED:
                return "闭环";
            default:
                return "未知状态";
        }
    }
}
