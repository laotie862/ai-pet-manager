package com.example.demo.behavior;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class CvInferenceClient {
    private final BehaviorProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CvInferenceClient(BehaviorProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(5, timeoutSeconds())))
                .build();
    }

    public BehaviorDetectionResponse detect(BehaviorDetectionRequest request) {
        return post("/cv/detect", request, BehaviorDetectionResponse.class);
    }

    public CvEmbeddingResponse embed(CvEmbeddingRequest request) {
        return post("/cv/embed", request, CvEmbeddingResponse.class);
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getCvBaseUrl().replaceAll("/+$", "") + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds()))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CV service returned " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CV service is unavailable");
        }
    }

    private int timeoutSeconds() {
        return Math.max(1, properties.getCvTimeoutSeconds());
    }
}
