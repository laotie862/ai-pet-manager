package com.example.demo.common.web;

import com.example.demo.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse("UP", Instant.now()));
    }

    public record HealthResponse(String status, Instant checkedAt) {
    }
}
