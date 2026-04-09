package com.example.demo.service;

public final class AlarmTypeBig {

    public static final int SHAFT_TEMP = 0;
    public static final int PARTIAL_DISCHARGE = 1;
    public static final int TRAVELING_WAVE_LOCATION = 2;
    public static final int BATTERY = 3;
    public static final int MAIN_TRANSFORMER_VIBRATION = 4;
    public static final int CORE_GROUND_CURRENT = 5;

    private AlarmTypeBig() {}

    public static String nameOf(Integer code) {
        if (code == null) {
            return null;
        }
        switch (code.intValue()) {
            case SHAFT_TEMP:
                return "竖井测温";
            case PARTIAL_DISCHARGE:
                return "配电柜局放";
            case TRAVELING_WAVE_LOCATION:
                return "行波定位";
            case BATTERY:
                return "蓄电池";
            case MAIN_TRANSFORMER_VIBRATION:
                return "主变振动";
            case CORE_GROUND_CURRENT:
                return "铁芯接地电流";
            default:
                return "未知大类";
        }
    }
}
