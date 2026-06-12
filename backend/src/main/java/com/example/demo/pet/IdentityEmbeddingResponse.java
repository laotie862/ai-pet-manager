package com.example.demo.pet;

import java.util.List;

public record IdentityEmbeddingResponse(
        List<Double> embedding,
        String modelVersion,
        int dimension
) {
}
