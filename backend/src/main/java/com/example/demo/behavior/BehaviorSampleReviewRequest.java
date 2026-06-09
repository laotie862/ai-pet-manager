package com.example.demo.behavior;

import jakarta.validation.constraints.NotBlank;

public record BehaviorSampleReviewRequest(
        @NotBlank
        String finalBehavior
) {
}
