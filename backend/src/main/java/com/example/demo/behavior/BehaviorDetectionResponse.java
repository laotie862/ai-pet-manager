package com.example.demo.behavior;

import java.util.List;
import java.util.Map;

public record BehaviorDetectionResponse(
        boolean found,
        String behavior,
        double confidence,
        List<Map<String, Object>> boxes,
        String modelVersion
) {
    public String normalizedBehavior(double minConfidence) {
        if (!found || confidence < minConfidence || behavior == null || behavior.isBlank()) {
            return "uncertain";
        }
        return behavior.trim().toLowerCase();
    }
}
