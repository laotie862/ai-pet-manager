package com.example.demo.behavior;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CvEmbeddingRequest(
        @JsonProperty("image_base64")
        String imageBase64
) {
}
