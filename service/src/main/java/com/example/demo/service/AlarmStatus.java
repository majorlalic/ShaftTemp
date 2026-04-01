package com.example.demo.service;

public final class AlarmStatus {

    public static final int PENDING_CONFIRM = 0;
    public static final int CONFIRMED = 1;
    public static final int OBSERVING = 2;
    public static final int FALSE_POSITIVE = 3;
    public static final int CLOSED = 4;
    public static final int AUTO_RECOVERED = 5;

    private AlarmStatus() {}

    public static String nameOf(Integer code) {
        if (code == null) {
            return null;
        }
        switch (code.intValue()) {
            case PENDING_CONFIRM:
                return "待确认";
            case CONFIRMED:
                return "已确认";
            case OBSERVING:
                return "持续观察";
            case FALSE_POSITIVE:
                return "误报";
            case CLOSED:
                return "已关闭";
            case AUTO_RECOVERED:
                return "自动恢复";
            default:
                return "未知状态";
        }
    }
}
