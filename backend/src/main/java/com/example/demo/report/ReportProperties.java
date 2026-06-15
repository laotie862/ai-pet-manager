package com.example.demo.report;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "petcare.report")
public class ReportProperties {
    private boolean qwenEnabled = true;
    private String qwenBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String qwenApiKey = "";
    private String qwenModel = "qwen-plus";
    private int qwenTimeoutSeconds = 30;
    private boolean weatherEnabled = true;
    private String weatherCity = "杭州";
    private Double weatherLatitude;
    private Double weatherLongitude;

    public boolean isQwenEnabled() {
        return qwenEnabled;
    }

    public void setQwenEnabled(boolean qwenEnabled) {
        this.qwenEnabled = qwenEnabled;
    }

    public String getQwenBaseUrl() {
        return qwenBaseUrl;
    }

    public void setQwenBaseUrl(String qwenBaseUrl) {
        this.qwenBaseUrl = qwenBaseUrl;
    }

    public String getQwenApiKey() {
        return qwenApiKey;
    }

    public void setQwenApiKey(String qwenApiKey) {
        this.qwenApiKey = qwenApiKey;
    }

    public String getQwenModel() {
        return qwenModel;
    }

    public void setQwenModel(String qwenModel) {
        this.qwenModel = qwenModel;
    }

    public int getQwenTimeoutSeconds() {
        return qwenTimeoutSeconds;
    }

    public void setQwenTimeoutSeconds(int qwenTimeoutSeconds) {
        this.qwenTimeoutSeconds = qwenTimeoutSeconds;
    }

    public boolean isWeatherEnabled() {
        return weatherEnabled;
    }

    public void setWeatherEnabled(boolean weatherEnabled) {
        this.weatherEnabled = weatherEnabled;
    }

    public String getWeatherCity() {
        return weatherCity;
    }

    public void setWeatherCity(String weatherCity) {
        this.weatherCity = weatherCity;
    }

    public Double getWeatherLatitude() {
        return weatherLatitude;
    }

    public void setWeatherLatitude(Double weatherLatitude) {
        this.weatherLatitude = weatherLatitude;
    }

    public Double getWeatherLongitude() {
        return weatherLongitude;
    }

    public void setWeatherLongitude(Double weatherLongitude) {
        this.weatherLongitude = weatherLongitude;
    }
}
