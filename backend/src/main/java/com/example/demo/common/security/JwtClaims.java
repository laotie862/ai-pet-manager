package com.example.demo.common.security;

import java.time.Instant;

public record JwtClaims(
        Long userId,
        String role,
        String tokenType,
        Instant issuedAt,
        Instant expiresAt
) {
}
