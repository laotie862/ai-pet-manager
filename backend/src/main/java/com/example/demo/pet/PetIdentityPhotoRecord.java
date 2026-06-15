package com.example.demo.pet;

import java.time.OffsetDateTime;

public record PetIdentityPhotoRecord(
        Long id,
        Long petId,
        String objectName,
        String imageUrl,
        String embeddingJson,
        String modelVersion,
        OffsetDateTime createdAt
) {
}
