package com.example.demo.auth;

import com.example.demo.user.UserResponse;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        UserResponse user
) {
}
