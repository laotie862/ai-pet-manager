package com.example.demo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(max = 255)
        String account,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
