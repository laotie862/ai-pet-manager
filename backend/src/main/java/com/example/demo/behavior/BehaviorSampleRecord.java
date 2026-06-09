package com.example.demo.behavior;

import java.time.OffsetDateTime;

public record BehaviorSampleRecord(
        Long id,
        Long petId,
        Long deviceId,
        Long eventId,
        String imagePath,
        String predictedBehavior,
        double confidence,
        boolean found,
        String provider,
        String modelVersion,
        String reviewStatus,
        String finalBehavior,
        OffsetDateTime capturedAt,
        OffsetDateTime createdAt,
        OffsetDateTime reviewedAt
) {
}
