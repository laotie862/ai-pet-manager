package com.example.demo.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "petcare")
public class AppProperties {
    private String serviceName = "AI Pet Care Backend";
    private String version = "0.1.0-stage1";
    private List<String> enabledModules = new ArrayList<>(Arrays.asList(
            "auth",
            "user",
            "pet",
            "device",
            "behavior",
            "notification",
            "admin"
    ));

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getEnabledModules() {
        return enabledModules;
    }

    public void setEnabledModules(List<String> enabledModules) {
        this.enabledModules = enabledModules;
    }
}
