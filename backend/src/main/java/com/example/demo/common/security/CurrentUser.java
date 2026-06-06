package com.example.demo.common.security;

public record CurrentUser(
        Long id,
        String email,
        String nickname,
        String role
) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
