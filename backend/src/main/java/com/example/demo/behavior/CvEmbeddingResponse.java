package com.example.demo.behavior;

import java.util.List;

public record CvEmbeddingResponse(
        List<Double> embedding,
        String modelVersion,
        int dimension
) {
}
