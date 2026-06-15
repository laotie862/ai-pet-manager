package com.example.demo.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class QwenReportClient {
    private final ReportProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public QwenReportClient(ReportProperties properties) {
        this.properties = properties;
    }

    public Optional<String> generate(String prompt) {
        if (!properties.isQwenEnabled() || !StringUtils.hasText(properties.getQwenApiKey())) {
            return Optional.empty();
        }

        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", properties.getQwenModel(),
                    "temperature", 0.4,
                    "max_tokens", 240,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "你是温和、谨慎的 AI 宠物健康日报助手。只基于给定数据分析，不编造诊断，不替代兽医。"
                            ),
                            Map.of("role", "user", "content", prompt)
                    )
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getQwenBaseUrl().replaceAll("/+$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(Math.max(5, properties.getQwenTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getQwenApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            JsonNode content = objectMapper.readTree(response.body())
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");
            if (!content.isTextual() || !StringUtils.hasText(content.asText())) {
                return Optional.empty();
            }
            return Optional.of(content.asText().trim());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
