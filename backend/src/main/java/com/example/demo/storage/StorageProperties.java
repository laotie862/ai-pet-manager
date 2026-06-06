package com.example.demo.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "petcare.storage")
public class StorageProperties {
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "petcare";
    private String publicBaseUrl = "http://localhost:9000/petcare";
    private String pathPrefix = "pet-photos";
    private long maxUploadBytes = 10 * 1024 * 1024;
    private boolean publicRead = true;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public boolean isPublicRead() {
        return publicRead;
    }

    public void setPublicRead(boolean publicRead) {
        this.publicRead = publicRead;
    }

    public String normalizedBucket() {
        return StringUtils.hasText(bucket) ? bucket.trim() : "petcare";
    }

    public String normalizedPathPrefix() {
        if (!StringUtils.hasText(pathPrefix)) {
            return "";
        }
        return pathPrefix.trim().replace("\\", "/").replaceAll("^/+|/+$", "");
    }

    public String normalizedPublicBaseUrl() {
        String baseUrl = StringUtils.hasText(publicBaseUrl) ? publicBaseUrl : "http://localhost:9000/" + normalizedBucket();
        return baseUrl.trim().replaceAll("/+$", "");
    }
}
