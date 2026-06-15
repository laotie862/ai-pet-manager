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
    private boolean staticSkipEnabled = true;
    private int minApiIntervalSeconds = 10;
    private int maxStaticApiIntervalSeconds = 300;
    private double motionThreshold = 0.05;
    private boolean sampleCaptureEnabled = true;
    private int sampleMinIntervalSeconds = 600;
    private int maxSamplesPerDeviceLabelPerDay = 300;
    private double autoTrainingConfidence = 0.9;
    private String sampleStoragePath = "/data/behavior-samples";
    private boolean cleanupEnabled = true;
    private int pendingReviewRetentionDays = 30;
    private String extractedFramesPath = "/data/extracted-frames";
    private int extractedFramesRetentionDays = 7;

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

    public boolean isStaticSkipEnabled() {
        return staticSkipEnabled;
    }

    public void setStaticSkipEnabled(boolean staticSkipEnabled) {
        this.staticSkipEnabled = staticSkipEnabled;
    }

    public int getMinApiIntervalSeconds() {
        return minApiIntervalSeconds;
    }

    public void setMinApiIntervalSeconds(int minApiIntervalSeconds) {
        this.minApiIntervalSeconds = minApiIntervalSeconds;
    }

    public int getMaxStaticApiIntervalSeconds() {
        return maxStaticApiIntervalSeconds;
    }

    public void setMaxStaticApiIntervalSeconds(int maxStaticApiIntervalSeconds) {
        this.maxStaticApiIntervalSeconds = maxStaticApiIntervalSeconds;
    }

    public double getMotionThreshold() {
        return motionThreshold;
    }

    public void setMotionThreshold(double motionThreshold) {
        this.motionThreshold = motionThreshold;
    }

    public boolean isSampleCaptureEnabled() {
        return sampleCaptureEnabled;
    }

    public void setSampleCaptureEnabled(boolean sampleCaptureEnabled) {
        this.sampleCaptureEnabled = sampleCaptureEnabled;
    }

    public int getSampleMinIntervalSeconds() {
        return sampleMinIntervalSeconds;
    }

    public void setSampleMinIntervalSeconds(int sampleMinIntervalSeconds) {
        this.sampleMinIntervalSeconds = sampleMinIntervalSeconds;
    }

    public int getMaxSamplesPerDeviceLabelPerDay() {
        return maxSamplesPerDeviceLabelPerDay;
    }

    public void setMaxSamplesPerDeviceLabelPerDay(int maxSamplesPerDeviceLabelPerDay) {
        this.maxSamplesPerDeviceLabelPerDay = maxSamplesPerDeviceLabelPerDay;
    }

    public double getAutoTrainingConfidence() {
        return autoTrainingConfidence;
    }

    public void setAutoTrainingConfidence(double autoTrainingConfidence) {
        this.autoTrainingConfidence = autoTrainingConfidence;
    }

    public String getSampleStoragePath() {
        return sampleStoragePath;
    }

    public void setSampleStoragePath(String sampleStoragePath) {
        this.sampleStoragePath = sampleStoragePath;
    }

    public boolean isCleanupEnabled() {
        return cleanupEnabled;
    }

    public void setCleanupEnabled(boolean cleanupEnabled) {
        this.cleanupEnabled = cleanupEnabled;
    }

    public int getPendingReviewRetentionDays() {
        return pendingReviewRetentionDays;
    }

    public void setPendingReviewRetentionDays(int pendingReviewRetentionDays) {
        this.pendingReviewRetentionDays = pendingReviewRetentionDays;
    }

    public String getExtractedFramesPath() {
        return extractedFramesPath;
    }

    public void setExtractedFramesPath(String extractedFramesPath) {
        this.extractedFramesPath = extractedFramesPath;
    }

    public int getExtractedFramesRetentionDays() {
        return extractedFramesRetentionDays;
    }

    public void setExtractedFramesRetentionDays(int extractedFramesRetentionDays) {
        this.extractedFramesRetentionDays = extractedFramesRetentionDays;
    }
}
