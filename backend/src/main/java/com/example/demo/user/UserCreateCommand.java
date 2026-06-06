package com.example.demo.user;

public record UserCreateCommand(
        String email,
        String phone,
        String passwordHash,
        String nickname,
        String role,
        String status
) {
}
