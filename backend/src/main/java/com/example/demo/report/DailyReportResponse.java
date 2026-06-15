package com.example.demo.report;

import com.example.demo.behavior.BehaviorSummaryResponse;

import java.time.LocalDate;

public record DailyReportResponse(
        Long petId,
        String petName,
        LocalDate reportDate,
        String content,
        String prompt,
        WeatherSnapshot weather,
        BehaviorSummaryResponse summary,
        boolean templateFallback,
        String modelVersion
) {
}
