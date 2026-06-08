package com.example.demo.device;

import java.time.OffsetDateTime;

public record DeviceStreamSnapshot(
        Long deviceId,
        DeviceStatus status,
        boolean running,
        boolean frameReady,
        OffsetDateTime lastFrameAt,
        String lastError
) {
    public static DeviceStreamSnapshot stopped(Long deviceId, DeviceStatus status, String lastError) {
        return new DeviceStreamSnapshot(deviceId, status, false, false, null, lastError);
    }
}
