package com.example.demo.behavior;

import java.time.OffsetDateTime;

public record BehaviorCurrentResponse(
        Long petId,
        Long deviceId,
        String behaviorType,
        double confidence,
        boolean found,
        OffsetDateTime startedAt,
        OffsetDateTime lastSeenAt,
        String modelVersion
) {
    public static BehaviorCurrentResponse uncertain(Long petId, Long deviceId) {
        return new BehaviorCurrentResponse(petId, deviceId, "uncertain", 0, false, null, null, null);
    }
}
