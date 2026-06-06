package com.example.demo.common.web;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.common.config.AppProperties;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemInfoController {
    private final AppProperties appProperties;
    private final Environment environment;

    public SystemInfoController(AppProperties appProperties, Environment environment) {
        this.appProperties = appProperties;
        this.environment = environment;
    }

    @GetMapping("/info")
    public ApiResponse<SystemInfoResponse> info() {
        return ApiResponse.success(new SystemInfoResponse(
                appProperties.getServiceName(),
                appProperties.getVersion(),
                appProperties.getEnabledModules(),
                Arrays.asList(environment.getActiveProfiles())
        ));
    }

    public record SystemInfoResponse(
            String serviceName,
            String version,
            List<String> enabledModules,
            List<String> activeProfiles
    ) {
    }
}
