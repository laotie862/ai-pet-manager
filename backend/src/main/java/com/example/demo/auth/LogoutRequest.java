package com.example.demo.auth;

public record LogoutRequest(
        String refreshToken
) {
}
