package com.example.demo.service;

public enum DeviceAssetStatus {
    PENDING_ACCESS("待接入"),
    CONNECTED("已接入");

    private final String value;

    DeviceAssetStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
