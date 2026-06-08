package com.example.demo.device;

import java.time.OffsetDateTime;

public record DeviceStatusResponse(
        Long deviceId,
        String status,
        boolean running,
        boolean frameReady,
        OffsetDateTime lastOnlineAt,
        OffsetDateTime lastHeartbeatAt,
        OffsetDateTime lastFrameAt,
        String lastError
) {
    public static DeviceStatusResponse from(DeviceRecord device, DeviceStreamSnapshot snapshot) {
        String status = snapshot.running() ? snapshot.status().name() : device.status().name();
        String lastError = snapshot.lastError() == null ? device.lastError() : snapshot.lastError();
        return new DeviceStatusResponse(
                device.id(),
                status,
                snapshot.running(),
                snapshot.frameReady(),
                device.lastOnlineAt(),
                device.lastHeartbeatAt(),
                snapshot.lastFrameAt(),
                lastError
        );
    }
}
