package com.example.demo.pet;

public record PetIdentityMatchResponse(
        Long petId,
        String petName,
        Long identityPhotoId,
        double similarity,
        boolean matched
) {
}
