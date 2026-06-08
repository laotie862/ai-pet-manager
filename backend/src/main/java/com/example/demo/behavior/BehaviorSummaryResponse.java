package com.example.demo.behavior;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BehaviorSummaryResponse(
        Long petId,
        LocalDate summaryDate,
        int eatingCount,
        int drinkingCount,
        int exercisingSeconds,
        int sleepingSeconds,
        int defecatingCount,
        OffsetDateTime updatedAt
) {
    public static BehaviorSummaryResponse empty(Long petId, LocalDate date) {
        return new BehaviorSummaryResponse(petId, date, 0, 0, 0, 0, 0, null);
    }

    public BehaviorSummaryResponse withLiveEvent(BehaviorEventRecord event, long durationSeconds) {
        int liveEating = "eating".equals(event.behaviorType()) ? 1 : 0;
        int liveDrinking = "drinking".equals(event.behaviorType()) ? 1 : 0;
        int liveExercising = "exercising".equals(event.behaviorType()) ? Math.toIntExact(Math.max(0, durationSeconds)) : 0;
        int liveSleeping = "sleeping".equals(event.behaviorType()) ? Math.toIntExact(Math.max(0, durationSeconds)) : 0;
        int liveDefecating = "defecating".equals(event.behaviorType()) ? 1 : 0;
        return new BehaviorSummaryResponse(
                petId,
                summaryDate,
                eatingCount + liveEating,
                drinkingCount + liveDrinking,
                exercisingSeconds + liveExercising,
                sleepingSeconds + liveSleeping,
                defecatingCount + liveDefecating,
                updatedAt
        );
    }
}
