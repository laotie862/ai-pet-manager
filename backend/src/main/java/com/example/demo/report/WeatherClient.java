package com.example.demo.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class WeatherClient {
    private final ReportProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public WeatherClient(ReportProperties properties) {
        this.properties = properties;
    }

    public WeatherSnapshot today() {
        if (!properties.isWeatherEnabled()) {
            return WeatherSnapshot.unavailable(properties.getWeatherCity());
        }

        try {
            Coordinates coordinates = resolveCoordinates();
            if (coordinates == null) {
                return WeatherSnapshot.unavailable(properties.getWeatherCity());
            }
            URI uri = URI.create("https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + coordinates.latitude()
                    + "&longitude=" + coordinates.longitude()
                    + "&current=temperature_2m,weather_code"
                    + "&timezone=auto");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return WeatherSnapshot.unavailable(properties.getWeatherCity());
            }
            JsonNode current = objectMapper.readTree(response.body()).path("current");
            double temp = current.path("temperature_2m").asDouble();
            int weatherCode = current.path("weather_code").asInt(-1);
            String condition = conditionText(weatherCode);
            return new WeatherSnapshot(
                    properties.getWeatherCity(),
                    condition,
                    temp,
                    adviceFor(condition, temp),
                    true
            );
        } catch (Exception ignored) {
            return WeatherSnapshot.unavailable(properties.getWeatherCity());
        }
    }

    private Coordinates resolveCoordinates() throws Exception {
        if (properties.getWeatherLatitude() != null && properties.getWeatherLongitude() != null) {
            return new Coordinates(properties.getWeatherLatitude(), properties.getWeatherLongitude());
        }
        if (properties.getWeatherCity() == null || properties.getWeatherCity().isBlank()) {
            return null;
        }
        URI uri = URI.create("https://geocoding-api.open-meteo.com/v1/search?count=1&language=zh&name="
                + java.net.URLEncoder.encode(properties.getWeatherCity(), java.nio.charset.StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }
        JsonNode first = objectMapper.readTree(response.body()).path("results").path(0);
        if (first.isMissingNode()) {
            return null;
        }
        return new Coordinates(first.path("latitude").asDouble(), first.path("longitude").asDouble());
    }

    private String conditionText(int code) {
        if (code == 0) return "晴";
        if (code <= 3) return "多云";
        if (code == 45 || code == 48) return "雾";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "雨";
        if (code >= 71 && code <= 77) return "雪";
        if (code >= 95) return "雷雨";
        return "未知";
    }

    private String adviceFor(String condition, double temp) {
        if (temp >= 30) {
            return "天气偏热，注意补水，避免长时间暴晒和高强度活动。";
        }
        if (temp <= 8) {
            return "天气偏冷，外出注意保暖，老年宠物和短毛宠物更要减少受凉。";
        }
        if ("雨".equals(condition) || "雷雨".equals(condition)) {
            return "雨天外出后及时擦干毛发和脚掌，减少皮肤潮湿。";
        }
        if ("雾".equals(condition)) {
            return "能见度和空气状态一般，建议减少户外奔跑时间。";
        }
        return "天气整体适中，可以保持规律活动和饮水观察。";
    }

    private record Coordinates(double latitude, double longitude) {
    }
}
