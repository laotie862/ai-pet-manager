package com.example.demo.device;

import java.time.OffsetDateTime;
import java.util.List;

public record DeviceResponse(
        Long id,
        Long petId,
        String name,
        String rtspUrl,
        String username,
        String status,
        List<RoiPoint> roiPolygon,
        OffsetDateTime lastOnlineAt,
        OffsetDateTime lastHeartbeatAt,
        String lastError,
        String streamPath,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DeviceResponse from(DeviceRecord device, List<RoiPoint> roiPolygon) {
        return new DeviceResponse(
                device.id(),
                device.petId(),
                device.name(),
                device.rtspUrl(),
                device.rtspUsername(),
                device.status().name(),
                roiPolygon,
                device.lastOnlineAt(),
                device.lastHeartbeatAt(),
                device.lastError(),
                "/ws/devices/" + device.id() + "/stream",
                device.createdAt(),
                device.updatedAt()
        );
    }
}
