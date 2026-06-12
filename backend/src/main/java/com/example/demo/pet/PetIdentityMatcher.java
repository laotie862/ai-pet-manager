package com.example.demo.pet;

import com.example.demo.behavior.BehaviorProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetIdentityMatcher {
    private static final TypeReference<List<Double>> EMBEDDING_TYPE = new TypeReference<>() {
    };

    private final PetIdentityRepository identityRepository;
    private final PetCvClient petCvClient;
    private final BehaviorProperties behaviorProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PetIdentityMatcher(
            PetIdentityRepository identityRepository,
            PetCvClient petCvClient,
            BehaviorProperties behaviorProperties
    ) {
        this.identityRepository = identityRepository;
        this.petCvClient = petCvClient;
        this.behaviorProperties = behaviorProperties;
    }

    public PetIdentityMatchDecision matches(Long petId, byte[] imageBytes) {
        List<PetIdentityPhotoRecord> photos = identityRepository.listByPet(petId);
        if (photos.isEmpty()) {
            return new PetIdentityMatchDecision(false, 0);
        }

        IdentityEmbeddingResponse target = petCvClient.embed(imageBytes);
        if (target.embedding() == null || target.embedding().isEmpty()) {
            return new PetIdentityMatchDecision(false, 0);
        }

        double bestSimilarity = photos.stream()
                .map(this::readEmbedding)
                .mapToDouble(embedding -> cosineSimilarity(embedding, target.embedding()))
                .max()
                .orElse(0);
        return new PetIdentityMatchDecision(
                bestSimilarity >= behaviorProperties.getIdentityMatchThreshold(),
                round(bestSimilarity)
        );
    }

    private List<Double> readEmbedding(PetIdentityPhotoRecord photo) {
        try {
            return objectMapper.readValue(photo.embeddingJson(), EMBEDDING_TYPE);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        int size = Math.min(left.size(), right.size());
        if (size == 0) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < size; index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    public record PetIdentityMatchDecision(boolean matched, double similarity) {
    }
}
