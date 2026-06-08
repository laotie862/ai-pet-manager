package com.example.demo.device;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "petcare.device")
public class DeviceProperties {
    private int freeLimit = 1;
    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";
    private int connectionTestTimeoutSeconds = 5;
    private boolean connectionTestEnabled = true;
    private boolean mockRtspEnabled = true;
    private boolean autoStartEnabled = true;
    private int streamFrameRate = 2;
    private int streamWidth = 640;
    private int restartDelaySeconds = 3;
    private int maxRestartAttempts = 5;
    private String localWebcamName;
    private long statusCacheTtlSeconds = 30;
    private String credentialSecret = "dev-stage3-device-secret-change-me-32-bytes";

    public int getFreeLimit() {
        return freeLimit;
    }

    public void setFreeLimit(int freeLimit) {
        this.freeLimit = freeLimit;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public String getFfprobePath() {
        return ffprobePath;
    }

    public void setFfprobePath(String ffprobePath) {
        this.ffprobePath = ffprobePath;
    }

    public int getConnectionTestTimeoutSeconds() {
        return connectionTestTimeoutSeconds;
    }

    public void setConnectionTestTimeoutSeconds(int connectionTestTimeoutSeconds) {
        this.connectionTestTimeoutSeconds = connectionTestTimeoutSeconds;
    }

    public boolean isConnectionTestEnabled() {
        return connectionTestEnabled;
    }

    public void setConnectionTestEnabled(boolean connectionTestEnabled) {
        this.connectionTestEnabled = connectionTestEnabled;
    }

    public boolean isMockRtspEnabled() {
        return mockRtspEnabled;
    }

    public void setMockRtspEnabled(boolean mockRtspEnabled) {
        this.mockRtspEnabled = mockRtspEnabled;
    }

    public boolean isAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    public int getStreamFrameRate() {
        return streamFrameRate;
    }

    public void setStreamFrameRate(int streamFrameRate) {
        this.streamFrameRate = streamFrameRate;
    }

    public int getStreamWidth() {
        return streamWidth;
    }

    public void setStreamWidth(int streamWidth) {
        this.streamWidth = streamWidth;
    }

    public int getRestartDelaySeconds() {
        return restartDelaySeconds;
    }

    public void setRestartDelaySeconds(int restartDelaySeconds) {
        this.restartDelaySeconds = restartDelaySeconds;
    }

    public int getMaxRestartAttempts() {
        return maxRestartAttempts;
    }

    public void setMaxRestartAttempts(int maxRestartAttempts) {
        this.maxRestartAttempts = maxRestartAttempts;
    }

    public String getLocalWebcamName() {
        return localWebcamName;
    }

    public void setLocalWebcamName(String localWebcamName) {
        this.localWebcamName = localWebcamName;
    }

    public long getStatusCacheTtlSeconds() {
        return statusCacheTtlSeconds;
    }

    public void setStatusCacheTtlSeconds(long statusCacheTtlSeconds) {
        this.statusCacheTtlSeconds = statusCacheTtlSeconds;
    }

    public String getCredentialSecret() {
        return credentialSecret;
    }

    public void setCredentialSecret(String credentialSecret) {
        this.credentialSecret = credentialSecret;
    }
}
