package com.example.demo.behavior;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "petcare.behavior")
public class BehaviorProperties {
    private boolean analysisEnabled = true;
    private long analysisIntervalMs = 5000;
    private String cvBaseUrl = "http://localhost:8000";
    private double minConfidence = 0.5;
    private int stableFrameCount = 3;
    private int minEventDurationSeconds = 5;

    public boolean isAnalysisEnabled() {
        return analysisEnabled;
    }

    public void setAnalysisEnabled(boolean analysisEnabled) {
        this.analysisEnabled = analysisEnabled;
    }

    public long getAnalysisIntervalMs() {
        return analysisIntervalMs;
    }

    public void setAnalysisIntervalMs(long analysisIntervalMs) {
        this.analysisIntervalMs = analysisIntervalMs;
    }

    public String getCvBaseUrl() {
        return cvBaseUrl;
    }

    public void setCvBaseUrl(String cvBaseUrl) {
        this.cvBaseUrl = cvBaseUrl;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public int getStableFrameCount() {
        return stableFrameCount;
    }

    public void setStableFrameCount(int stableFrameCount) {
        this.stableFrameCount = stableFrameCount;
    }

    public int getMinEventDurationSeconds() {
        return minEventDurationSeconds;
    }

    public void setMinEventDurationSeconds(int minEventDurationSeconds) {
        this.minEventDurationSeconds = minEventDurationSeconds;
    }
}
