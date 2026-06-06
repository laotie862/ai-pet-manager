package com.example.demo.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "petcare.security")
public class SecurityProperties {
    private String jwtSecret = "dev-stage2-secret-change-me-please-32-bytes";
    private long accessTokenMinutes = 30;
    private long refreshTokenDays = 14;
    private boolean adminBootstrapEnabled = true;
    private String adminEmail = "admin@example.com";
    private String adminPassword = "Admin@123456";
    private String adminNickname = "Platform Admin";

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    public void setRefreshTokenDays(long refreshTokenDays) {
        this.refreshTokenDays = refreshTokenDays;
    }

    public boolean isAdminBootstrapEnabled() {
        return adminBootstrapEnabled;
    }

    public void setAdminBootstrapEnabled(boolean adminBootstrapEnabled) {
        this.adminBootstrapEnabled = adminBootstrapEnabled;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminNickname() {
        return adminNickname;
    }

    public void setAdminNickname(String adminNickname) {
        this.adminNickname = adminNickname;
    }
}
