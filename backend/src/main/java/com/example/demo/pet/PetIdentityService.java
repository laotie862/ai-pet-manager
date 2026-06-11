package com.example.demo.pet;

import com.example.demo.behavior.CvEmbeddingRequest;
import com.example.demo.behavior.CvEmbeddingResponse;
import com.example.demo.behavior.CvInferenceClient;
import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.storage.ObjectStorageService;
import com.example.demo.storage.StoredObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
public class PetIdentityService {
    // First-pass threshold for local color/spatial embeddings; tune after real pet photos are collected.
    private static final double IDENTITY_MATCH_THRESHOLD = 0.78;

    private final PetRepository petRepository;
    private final PetIdentityPhotoRepository identityPhotoRepository;
    private final ObjectStorageService objectStorageService;
    private final CvInferenceClient cvInferenceClient;

    public PetIdentityService(
            PetRepository petRepository,
            PetIdentityPhotoRepository identityPhotoRepository,
            ObjectStorageService objectStorageService,
            CvInferenceClient cvInferenceClient
    ) {
        this.petRepository = petRepository;
        this.identityPhotoRepository = identityPhotoRepository;
        this.objectStorageService = objectStorageService;
        this.cvInferenceClient = cvInferenceClient;
    }

    @Transactional
    public PetIdentityPhotoResponse uploadIdentityPhoto(Long petId, MultipartFile file) {
        Long userId = SecurityUtils.currentUser().id();
        ensurePetBelongsToUser(petId, userId);

        byte[] imageBytes = readBytes(file);
        CvEmbeddingResponse embeddingResponse = embedImage(imageBytes, "Identity photo cannot be embedded");
        StoredObject storedObject = objectStorageService.uploadPetPhoto(userId, petId, file);

        PetIdentityPhotoRecord record = identityPhotoRepository.create(
                petId,
                storedObject.objectName(),
                storedObject.url(),
                embeddingResponse.embedding(),
                embeddingResponse.modelVersion()
        );
        return PetIdentityPhotoResponse.from(record);
    }

    public List<PetIdentityPhotoResponse> listIdentityPhotos(Long petId) {
        Long userId = SecurityUtils.currentUser().id();
        ensurePetBelongsToUser(petId, userId);
        return identityPhotoRepository.listByPet(petId).stream()
                .map(PetIdentityPhotoResponse::from)
                .toList();
    }

    @Transactional
    public void deleteIdentityPhoto(Long petId, Long photoId) {
        Long userId = SecurityUtils.currentUser().id();
        ensurePetBelongsToUser(petId, userId);
        int deleted = identityPhotoRepository.delete(photoId, petId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Identity photo not found");
        }
    }

    public PetIdentityMatchResponse matchIdentity(MultipartFile file) {
        Long userId = SecurityUtils.currentUser().id();
        CvEmbeddingResponse embeddingResponse = embedImage(readBytes(file), "Image cannot be embedded");

        List<PetIdentityPhotoRecord> candidates = identityPhotoRepository.listByUser(userId);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "No pet identity photos found");
        }

        return candidates.stream()
                .map(candidate -> toMatchResponse(candidate, embeddingResponse.embedding(), userId))
                .max(Comparator.comparingDouble(PetIdentityMatchResponse::similarity))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "No pet identity photos found"));
    }

    private CvEmbeddingResponse embedImage(byte[] imageBytes, String emptyEmbeddingMessage) {
        CvEmbeddingResponse embeddingResponse = cvInferenceClient.embed(
                new CvEmbeddingRequest(Base64.getEncoder().encodeToString(imageBytes))
        );
        if (embeddingResponse.embedding() == null || embeddingResponse.embedding().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, emptyEmbeddingMessage);
        }
        return embeddingResponse;
    }

    private PetIdentityMatchResponse toMatchResponse(
            PetIdentityPhotoRecord candidate,
            List<Double> queryEmbedding,
            Long userId
    ) {
        PetRecord pet = petRepository.findByIdAndUser(candidate.petId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
        double similarity = cosineSimilarity(queryEmbedding, candidate.embedding());
        return new PetIdentityMatchResponse(
                pet.id(),
                pet.name(),
                candidate.id(),
                Math.round(similarity * 10000.0) / 10000.0,
                similarity >= IDENTITY_MATCH_THRESHOLD
        );
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        int size = Math.min(left.size(), right.size());
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < size; i++) {
            double leftValue = safeDouble(left.get(i));
            double rightValue = safeDouble(right.get(i));
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double safeDouble(Double value) {
        return value == null || value.isNaN() || value.isInfinite() ? 0.0 : value;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Image file is required");
            }
            return file.getBytes();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Image file cannot be read");
        }
    }

    private void ensurePetBelongsToUser(Long petId, Long userId) {
        petRepository.findByIdAndUser(petId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
    }
}
