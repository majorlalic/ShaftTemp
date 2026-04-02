package com.example.demo.service;

public final class AlarmEventType {

    public static final int TRIGGER = 0;
    public static final int MERGE = 1;
    public static final int CONFIRM = 2;
    public static final int OBSERVE = 3;
    public static final int FALSE_POSITIVE = 4;
    public static final int RECOVER = 5;
    public static final int CLOSE = 6;

    private AlarmEventType() {}

    public static String nameOf(Integer code) {
        if (code == null) {
            return null;
        }
        switch (code.intValue()) {
            case TRIGGER:
                return "首次触发";
            case MERGE:
                return "合并触发";
            case CONFIRM:
                return "人工确认";
            case OBSERVE:
                return "持续观察";
            case FALSE_POSITIVE:
                return "误报";
            case RECOVER:
                return "恢复";
            case CLOSE:
                return "关闭";
            default:
                return "未知事件";
        }
    }
}
