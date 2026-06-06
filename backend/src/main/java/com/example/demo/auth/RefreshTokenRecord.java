package com.example.demo.auth;

import java.time.OffsetDateTime;

public record RefreshTokenRecord(
        Long id,
        Long userId,
        String tokenHash,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt
) {
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
}
