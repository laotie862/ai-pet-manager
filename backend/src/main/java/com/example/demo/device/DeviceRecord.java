package com.example.demo.device;

import java.time.OffsetDateTime;

public record DeviceRecord(
        Long id,
        Long userId,
        Long petId,
        String name,
        String rtspUrl,
        String rtspUsername,
        String rtspPasswordCipher,
        String streamKey,
        DeviceStatus status,
        String roiPolygonJson,
        OffsetDateTime lastOnlineAt,
        OffsetDateTime lastHeartbeatAt,
        String lastError,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
