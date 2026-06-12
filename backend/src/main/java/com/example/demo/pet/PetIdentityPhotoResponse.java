package com.example.demo.pet;

import java.time.OffsetDateTime;

public record PetIdentityPhotoResponse(
        Long id,
        Long petId,
        String imageUrl,
        String modelVersion,
        int embeddingDimension,
        OffsetDateTime createdAt
) {
    public static PetIdentityPhotoResponse from(PetIdentityPhotoRecord photo, int embeddingDimension) {
        return new PetIdentityPhotoResponse(
                photo.id(),
                photo.petId(),
                photo.imageUrl(),
                photo.modelVersion(),
                embeddingDimension,
                photo.createdAt()
        );
    }
}
