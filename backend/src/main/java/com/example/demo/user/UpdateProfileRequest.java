package com.example.demo.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 64)
        String nickname,

        @Size(max = 32)
        String phone
) {
}
