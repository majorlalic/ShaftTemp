package com.example.demo.service;

public final class AlarmStatus {

    public static final int PENDING_CONFIRM = 0;
    public static final int OBSERVING = 1;
    public static final int PENDING_RECTIFICATION = 2;
    public static final int PENDING_RETEST = 3;
    public static final int CONFIRMED = 4;
    public static final int CLOSED = 5;
    // 兼容旧逻辑：旧“误报”落到新“待复测”
    public static final int FALSE_POSITIVE = PENDING_RETEST;
    // 兼容旧逻辑：旧“自动恢复”落到新“闭环”
    public static final int AUTO_RECOVERED = CLOSED;

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
