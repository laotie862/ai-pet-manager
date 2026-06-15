package com.example.demo.pet;

import com.example.demo.behavior.BehaviorProperties;
import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Component
public class PetCvClient {
    private final BehaviorProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PetCvClient(BehaviorProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public IdentityEmbeddingResponse embed(MultipartFile file) {
        try {
            return embed(file.getBytes());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CV embedding service is unavailable");
        }
    }

    public IdentityEmbeddingResponse embed(byte[] imageBytes) {
        try {
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
            String body = objectMapper.writeValueAsString(new IdentityEmbeddingRequest(imageBase64));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getCvBaseUrl().replaceAll("/+$", "") + "/cv/embed"))
                    .timeout(Duration.ofSeconds(properties.getCvTimeoutSeconds()))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CV embedding service returned " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), IdentityEmbeddingResponse.class);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CV embedding service is unavailable");
        }
    }
}
