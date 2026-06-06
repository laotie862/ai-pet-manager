package com.example.demo.user;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String email,
        String phone,
        String nickname,
        String role,
        String status,
        boolean vip,
        OffsetDateTime vipUntil,
        OffsetDateTime createdAt
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.phone(),
                user.nickname(),
                user.role(),
                user.status(),
                user.isVip(),
                user.vipUntil(),
                user.createdAt()
        );
    }
}
