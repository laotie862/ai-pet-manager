package com.example.demo.report;

public record WeatherSnapshot(
        String city,
        String condition,
        Double temperatureCelsius,
        String advice,
        boolean available
) {
    public static WeatherSnapshot unavailable(String city) {
        return new WeatherSnapshot(city, "unknown", null, "天气暂不可用，建议按室内环境观察宠物状态。", false);
    }
}
