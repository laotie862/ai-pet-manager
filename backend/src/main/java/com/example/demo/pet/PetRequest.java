package com.example.demo.pet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank
        @Size(max = 64)
        String name,

        @NotBlank
        @Size(max = 32)
        String species,

        @Size(max = 64)
        String breed,

        @Size(max = 16)
        String gender,

        LocalDate birthday,

        @Size(max = 512)
        String avatarUrl,

        @DecimalMin("0.1")
        BigDecimal weightKg
) {
}
