package com.example.demo.pet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PetRecord(
        Long id,
        Long userId,
        String name,
        String species,
        String breed,
        String gender,
        LocalDate birthday,
        String avatarUrl,
        BigDecimal weightKg,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
