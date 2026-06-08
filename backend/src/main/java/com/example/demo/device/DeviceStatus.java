package com.example.demo.device;

public enum DeviceStatus {
    ONLINE,
    OFFLINE,
    ANALYZING;

    public static DeviceStatus from(String value) {
        if (value == null) {
            return OFFLINE;
        }
        for (DeviceStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return OFFLINE;
    }
}
