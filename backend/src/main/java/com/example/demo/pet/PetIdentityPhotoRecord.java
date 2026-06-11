package com.example.demo.pet;

import java.time.OffsetDateTime;
import java.util.List;

public record PetIdentityPhotoRecord(
        Long id,
        Long petId,
        String objectName,
        String imageUrl,
        List<Double> embedding,
        String modelVersion,
        OffsetDateTime createdAt
) {
}
