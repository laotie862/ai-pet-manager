package com.example.demo.behavior;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.pet.PetRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.List;

@Service
public class BehaviorSampleService {
    private static final Set<String> BEHAVIORS = Set.of(
            "eating", "drinking", "exercising", "sleeping", "defecating", "uncertain"
    );

    private final PetRepository petRepository;
    private final BehaviorSampleRepository sampleRepository;
    private final BehaviorTrainingDatasetService trainingDatasetService;

    public BehaviorSampleService(
            PetRepository petRepository,
            BehaviorSampleRepository sampleRepository,
            BehaviorTrainingDatasetService trainingDatasetService
    ) {
        this.petRepository = petRepository;
        this.sampleRepository = sampleRepository;
        this.trainingDatasetService = trainingDatasetService;
    }

    public List<BehaviorSampleRecord> listByPet(Long petId, int limit) {
        ensureOwnedPet(petId);
        return sampleRepository.listByPet(petId, limit);
    }

    public void review(Long petId, Long sampleId, String finalBehavior) {
        ensureOwnedPet(petId);
        String normalized = normalizeBehavior(finalBehavior);
        BehaviorSampleRecord sample = sampleRepository.findByIdAndPet(sampleId, petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Behavior sample not found"));
        OffsetDateTime reviewedAt = OffsetDateTime.now(ZoneOffset.UTC);
        int updated = sampleRepository.reviewSample(sampleId, petId, normalized, reviewedAt);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Behavior sample not found");
        }
        trainingDatasetService.copyCandidate(
                Paths.get(sample.imagePath()),
                normalized,
                sample.petId(),
                sample.deviceId(),
                sample.capturedAt() == null ? reviewedAt : sample.capturedAt()
        );
    }

    private void ensureOwnedPet(Long petId) {
        Long userId = SecurityUtils.currentUser().id();
        petRepository.findByIdAndUser(petId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
    }

    private String normalizeBehavior(String behavior) {
        if (behavior == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Behavior is required");
        }
        String normalized = behavior.trim().toLowerCase();
        if (!BEHAVIORS.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported behavior: " + behavior);
        }
        return normalized;
    }
}
