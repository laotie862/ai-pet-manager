package com.example.demo.pet;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.storage.ObjectStorageService;
import com.example.demo.storage.StoredObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@Service
public class PetIdentityService {
    private static final double MATCH_THRESHOLD = 0.90;

    private final PetRepository petRepository;
    private final PetIdentityRepository identityRepository;
    private final ObjectStorageService objectStorageService;
    private final PetCvClient petCvClient;
    private final ObjectMapper objectMapper;

    public PetIdentityService(
            PetRepository petRepository,
            PetIdentityRepository identityRepository,
            ObjectStorageService objectStorageService,
            PetCvClient petCvClient
    ) {
        this.petRepository = petRepository;
        this.identityRepository = identityRepository;
        this.objectStorageService = objectStorageService;
        this.petCvClient = petCvClient;
        this.objectMapper = new ObjectMapper();
    }

    public List<PetIdentityPhotoResponse> list(Long petId) {
        ensureOwnedPet(petId);
        return identityRepository.listByPet(petId).stream()
                .map(photo -> PetIdentityPhotoResponse.from(photo, readEmbedding(photo).size()))
                .toList();
    }

    @Transactional
    public PetIdentityPhotoResponse upload(Long petId, MultipartFile file) {
        Long userId = SecurityUtils.currentUser().id();
        ensureOwnedPet(petId);
        IdentityEmbeddingResponse embedding = petCvClient.embed(file);
        if (embedding.embedding() == null || embedding.embedding().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法从照片提取宠物身份特征");
        }

        StoredObject storedObject = objectStorageService.uploadPetIdentityPhoto(userId, petId, file);
        String embeddingJson = writeEmbedding(embedding.embedding());
        Long photoId = identityRepository.create(
                petId,
                storedObject.objectName(),
                storedObject.url(),
                embeddingJson,
                embedding.modelVersion()
        );
        PetIdentityPhotoRecord saved = identityRepository.listByPet(petId).stream()
                .filter(photo -> photo.id().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Identity photo save failed"));
        return PetIdentityPhotoResponse.from(saved, embedding.embedding().size());
    }

    @Transactional
    public void delete(Long petId, Long photoId) {
        ensureOwnedPet(petId);
        int deleted = identityRepository.delete(petId, photoId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Identity photo not found");
        }
    }

    public PetIdentityMatchResponse match(MultipartFile file) {
        Long userId = SecurityUtils.currentUser().id();
        IdentityEmbeddingResponse target = petCvClient.embed(file);
        if (target.embedding() == null || target.embedding().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法从测试图片提取宠物身份特征");
        }

        return identityRepository.listByUser(userId).stream()
                .map(photo -> toCandidate(photo, target.embedding()))
                .max(Comparator.comparingDouble(MatchCandidate::similarity))
                .map(candidate -> toResponse(userId, candidate))
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "请先上传宠物身份照片"));
    }

    private PetIdentityMatchResponse toResponse(Long userId, MatchCandidate candidate) {
        PetRecord pet = petRepository.findByIdAndUser(candidate.photo().petId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
        return new PetIdentityMatchResponse(
                pet.id(),
                pet.name(),
                candidate.photo().id(),
                round(candidate.similarity()),
                candidate.similarity() >= MATCH_THRESHOLD
        );
    }

    private MatchCandidate toCandidate(PetIdentityPhotoRecord photo, List<Double> targetEmbedding) {
        return new MatchCandidate(photo, cosineSimilarity(readEmbedding(photo), targetEmbedding));
    }

    private void ensureOwnedPet(Long petId) {
        Long userId = SecurityUtils.currentUser().id();
        petRepository.findByIdAndUser(petId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
    }

    private List<Double> readEmbedding(PetIdentityPhotoRecord photo) {
        try {
            return objectMapper.readValue(photo.embeddingJson(), new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Identity embedding is invalid");
        }
    }

    private String writeEmbedding(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Identity embedding save failed");
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

    private record MatchCandidate(PetIdentityPhotoRecord photo, double similarity) {
    }
}
