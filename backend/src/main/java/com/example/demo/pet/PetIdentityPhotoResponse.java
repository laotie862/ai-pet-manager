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
    public static PetIdentityPhotoResponse from(PetIdentityPhotoRecord record) {
        return new PetIdentityPhotoResponse(
                record.id(),
                record.petId(),
                record.imageUrl(),
                record.modelVersion(),
                record.embedding() == null ? 0 : record.embedding().size(),
                record.createdAt()
        );
    }
}
