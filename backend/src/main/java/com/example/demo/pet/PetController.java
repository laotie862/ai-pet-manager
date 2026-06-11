package com.example.demo.pet;

import com.example.demo.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {
    private final PetService petService;
    private final PetIdentityService petIdentityService;

    public PetController(PetService petService, PetIdentityService petIdentityService) {
        this.petService = petService;
        this.petIdentityService = petIdentityService;
    }

    @GetMapping
    public ApiResponse<List<PetResponse>> list() {
        return ApiResponse.success(petService.list());
    }

    @PostMapping
    public ApiResponse<PetResponse> create(@Valid @RequestBody PetRequest request) {
        return ApiResponse.success(petService.create(request));
    }

    @GetMapping("/{petId}")
    public ApiResponse<PetResponse> detail(@PathVariable Long petId) {
        return ApiResponse.success(petService.detail(petId));
    }

    @PutMapping("/{petId}")
    public ApiResponse<PetResponse> update(@PathVariable Long petId, @Valid @RequestBody PetRequest request) {
        return ApiResponse.success(petService.update(petId, request));
    }

    @PostMapping(path = "/{petId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PetResponse> uploadPhoto(@PathVariable Long petId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(petService.uploadPhoto(petId, file));
    }

    @PostMapping(path = "/{petId}/identity-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PetIdentityPhotoResponse> uploadIdentityPhoto(
            @PathVariable Long petId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(petIdentityService.uploadIdentityPhoto(petId, file));
    }

    @GetMapping("/{petId}/identity-photos")
    public ApiResponse<List<PetIdentityPhotoResponse>> listIdentityPhotos(@PathVariable Long petId) {
        return ApiResponse.success(petIdentityService.listIdentityPhotos(petId));
    }

    @DeleteMapping("/{petId}/identity-photos/{photoId}")
    public ApiResponse<Void> deleteIdentityPhoto(@PathVariable Long petId, @PathVariable Long photoId) {
        petIdentityService.deleteIdentityPhoto(petId, photoId);
        return ApiResponse.success();
    }

    @PostMapping(path = "/identity/match", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PetIdentityMatchResponse> matchIdentity(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(petIdentityService.matchIdentity(file));
    }

    @DeleteMapping("/{petId}")
    public ApiResponse<Void> delete(@PathVariable Long petId) {
        petService.delete(petId);
        return ApiResponse.success();
    }
}
