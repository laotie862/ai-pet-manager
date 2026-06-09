package com.example.demo.behavior;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "petcare.behavior")
public class BehaviorProperties {
    private boolean analysisEnabled = true;
    private long analysisIntervalMs = 30000;
    private String cvBaseUrl = "http://localhost:8000";
    private int cvTimeoutSeconds = 30;
    private double minConfidence = 0.5;
    private int stableFrameCount = 3;
    private int minEventDurationSeconds = 5;
    private boolean staticSkipEnabled = true;
    private long minApiIntervalSeconds = 30;
    private long maxStaticApiIntervalSeconds = 300;
    private double motionThreshold = 0.05;
    private boolean sampleCaptureEnabled = true;
    private long sampleMinIntervalSeconds = 600;
    private int maxSamplesPerDeviceLabelPerDay = 300;
    private String sampleStoragePath = "dataset/behavior-samples";
    private String trainingDatasetPath = "dataset/behavior-classifier";
    private double autoTrainingConfidence = 0.85;
    private boolean cleanupEnabled = true;
    private int pendingReviewRetentionDays = 30;
    private String extractedFramesPath = "dataset/extracted-frames";
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

    public int getMinEventDurationSeconds() {
        return minEventDurationSeconds;
    }

    public void setMinEventDurationSeconds(int minEventDurationSeconds) {
        this.minEventDurationSeconds = minEventDurationSeconds;
    }

    public boolean isStaticSkipEnabled() {
        return staticSkipEnabled;
    }

    public void setStaticSkipEnabled(boolean staticSkipEnabled) {
        this.staticSkipEnabled = staticSkipEnabled;
    }

    public long getMinApiIntervalSeconds() {
        return minApiIntervalSeconds;
    }

    public void setMinApiIntervalSeconds(long minApiIntervalSeconds) {
        this.minApiIntervalSeconds = minApiIntervalSeconds;
    }

    public long getMaxStaticApiIntervalSeconds() {
        return maxStaticApiIntervalSeconds;
    }

    public void setMaxStaticApiIntervalSeconds(long maxStaticApiIntervalSeconds) {
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

    public long getSampleMinIntervalSeconds() {
        return sampleMinIntervalSeconds;
    }

    public void setSampleMinIntervalSeconds(long sampleMinIntervalSeconds) {
        this.sampleMinIntervalSeconds = sampleMinIntervalSeconds;
    }

    public int getMaxSamplesPerDeviceLabelPerDay() {
        return maxSamplesPerDeviceLabelPerDay;
    }

    public void setMaxSamplesPerDeviceLabelPerDay(int maxSamplesPerDeviceLabelPerDay) {
        this.maxSamplesPerDeviceLabelPerDay = maxSamplesPerDeviceLabelPerDay;
    }

    public String getSampleStoragePath() {
        return sampleStoragePath;
    }

    public void setSampleStoragePath(String sampleStoragePath) {
        this.sampleStoragePath = sampleStoragePath;
    }

    public String getTrainingDatasetPath() {
        return trainingDatasetPath;
    }

    public void setTrainingDatasetPath(String trainingDatasetPath) {
        this.trainingDatasetPath = trainingDatasetPath;
    }

    public double getAutoTrainingConfidence() {
        return autoTrainingConfidence;
    }

    public void setAutoTrainingConfidence(double autoTrainingConfidence) {
        this.autoTrainingConfidence = autoTrainingConfidence;
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
