package com.example.demo.pet;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.CurrentUser;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.storage.ObjectStorageService;
import com.example.demo.storage.StoredObject;
import com.example.demo.user.UserAccount;
import com.example.demo.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class PetService {
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetProperties petProperties;
    private final ObjectStorageService objectStorageService;

    public PetService(
            PetRepository petRepository,
            UserRepository userRepository,
            PetProperties petProperties,
            ObjectStorageService objectStorageService
    ) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.petProperties = petProperties;
        this.objectStorageService = objectStorageService;
    }

    public List<PetResponse> list() {
        Long userId = SecurityUtils.currentUser().id();
        return petRepository.listByUser(userId).stream().map(PetResponse::from).toList();
    }

    public PetResponse detail(Long petId) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return petRepository.findByIdAndUser(petId, currentUser.id())
                .map(PetResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
    }

    @Transactional
    public PetResponse create(PetRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        UserAccount user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!user.isVip() && petRepository.countByUser(user.id()) >= petProperties.getFreeLimit()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Free users can create up to " + petProperties.getFreeLimit() + " pets");
        }
        Long petId = petRepository.create(user.id(), request);
        return detail(petId);
    }

    @Transactional
    public PetResponse update(Long petId, PetRequest request) {
        Long userId = SecurityUtils.currentUser().id();
        ensureExists(petId, userId);
        return PetResponse.from(petRepository.update(petId, userId, request));
    }

    @Transactional
    public PetResponse uploadPhoto(Long petId, MultipartFile file) {
        Long userId = SecurityUtils.currentUser().id();
        ensureExists(petId, userId);
        StoredObject storedObject = objectStorageService.uploadPetPhoto(userId, petId, file);
        return petRepository.updateAvatarUrl(petId, userId, storedObject.url())
                .map(PetResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
    }

    @Transactional
    public void delete(Long petId) {
        Long userId = SecurityUtils.currentUser().id();
        int deleted = petRepository.delete(petId, userId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Pet not found");
        }
    }

    private void ensureExists(Long petId, Long userId) {
        petRepository.findByIdAndUser(petId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
    }
}
