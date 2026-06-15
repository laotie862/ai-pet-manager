package com.example.demo.behavior;

import java.time.OffsetDateTime;

/**
 * A single behavior sample persisted to t_behavior_sample.
 * imagePath is relative to the sample storage root so the same value
 * works across Docker, Windows and future server migrations.
 */
public record BehaviorSampleRecord(
        Long id,
        Long petId,
        Long deviceId,
        String behaviorType,
        double confidence,
        String imagePath,
        String reviewStatus,
        String finalBehavior,
        String modelVersion,
        OffsetDateTime capturedAt,
        OffsetDateTime reviewedAt,
        OffsetDateTime createdAt
) {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_AUTO_APPROVED = "AUTO_APPROVED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_DISCARDED = "DISCARDED";
}
