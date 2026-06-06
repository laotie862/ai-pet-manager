package com.example.demo.user;

import java.time.OffsetDateTime;

public record UserAccount(
        Long id,
        String email,
        String phone,
        String passwordHash,
        String nickname,
        String role,
        String status,
        OffsetDateTime vipUntil,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isVip() {
        return vipUntil != null && vipUntil.isAfter(OffsetDateTime.now());
    }
}
