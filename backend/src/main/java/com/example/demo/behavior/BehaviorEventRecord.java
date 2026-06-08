package com.example.demo.behavior;

import java.time.OffsetDateTime;

public record BehaviorEventRecord(
        Long id,
        Long petId,
        Long deviceId,
        String behaviorType,
        double confidence,
        boolean found,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String modelVersion,
        OffsetDateTime createdAt
) {
}
