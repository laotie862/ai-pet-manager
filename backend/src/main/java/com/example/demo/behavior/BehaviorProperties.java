package com.example.demo.behavior;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "petcare.behavior")
public class BehaviorProperties {
    private boolean analysisEnabled = true;
    private long analysisIntervalMs = 5000;
    private String cvBaseUrl = "http://localhost:8000";
    private int cvTimeoutSeconds = 45;
    private double minConfidence = 0.5;
    private int stableFrameCount = 3;
    private int quickEventStableFrameCount = 1;
    private int uncertainCloseFrameCount = 2;
    private int minEventDurationSeconds = 5;
    private double quickEventMinConfidence = 0.4;
    private String quickEventBehaviors = "eating,drinking,defecating";
    private boolean identityMatchEnabled = true;
    private double identityMatchThreshold = 0.75;

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

    public int getCvTimeoutSeconds() {
        return cvTimeoutSeconds;
    }

    public void setCvTimeoutSeconds(int cvTimeoutSeconds) {
        this.cvTimeoutSeconds = cvTimeoutSeconds;
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

    public int getQuickEventStableFrameCount() {
        return quickEventStableFrameCount;
    }

    public void setQuickEventStableFrameCount(int quickEventStableFrameCount) {
        this.quickEventStableFrameCount = quickEventStableFrameCount;
    }

    public int getUncertainCloseFrameCount() {
        return uncertainCloseFrameCount;
    }

    public void setUncertainCloseFrameCount(int uncertainCloseFrameCount) {
        this.uncertainCloseFrameCount = uncertainCloseFrameCount;
    }

    public int getMinEventDurationSeconds() {
        return minEventDurationSeconds;
    }

    public void setMinEventDurationSeconds(int minEventDurationSeconds) {
        this.minEventDurationSeconds = minEventDurationSeconds;
    }

    public double getQuickEventMinConfidence() {
        return quickEventMinConfidence;
    }

    public void setQuickEventMinConfidence(double quickEventMinConfidence) {
        this.quickEventMinConfidence = quickEventMinConfidence;
    }

    public String getQuickEventBehaviors() {
        return quickEventBehaviors;
    }

    public void setQuickEventBehaviors(String quickEventBehaviors) {
        this.quickEventBehaviors = quickEventBehaviors;
    }

    public boolean isIdentityMatchEnabled() {
        return identityMatchEnabled;
    }

    public void setIdentityMatchEnabled(boolean identityMatchEnabled) {
        this.identityMatchEnabled = identityMatchEnabled;
    }

    public double getIdentityMatchThreshold() {
        return identityMatchThreshold;
    }

    public void setIdentityMatchThreshold(double identityMatchThreshold) {
        this.identityMatchThreshold = identityMatchThreshold;
    }
}
