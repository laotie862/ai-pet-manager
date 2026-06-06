package com.example.demo.pet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PetResponse(
        Long id,
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
    public static PetResponse from(PetRecord pet) {
        return new PetResponse(
                pet.id(),
                pet.name(),
                pet.species(),
                pet.breed(),
                pet.gender(),
                pet.birthday(),
                pet.avatarUrl(),
                pet.weightKg(),
                pet.createdAt(),
                pet.updatedAt()
        );
    }
}
