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
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public BehaviorDetectionResponse detect(BehaviorDetectionRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getCvBaseUrl().replaceAll("/+$", "") + "/cv/detect"))
                    .timeout(Duration.ofSeconds(properties.getCvTimeoutSeconds()))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CV service returned " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), BehaviorDetectionResponse.class);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CV service is unavailable");
        }
    }
}
