package com.example.demo.pet;

import com.example.demo.common.api.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetIdentityController {
    private final PetIdentityService petIdentityService;

    public PetIdentityController(PetIdentityService petIdentityService) {
        this.petIdentityService = petIdentityService;
    }

    @GetMapping("/{petId}/identity-photos")
    public ApiResponse<List<PetIdentityPhotoResponse>> list(@PathVariable Long petId) {
        return ApiResponse.success(petIdentityService.list(petId));
    }

    @PostMapping(path = "/{petId}/identity-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PetIdentityPhotoResponse> upload(
            @PathVariable Long petId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(petIdentityService.upload(petId, file));
    }

    @DeleteMapping("/{petId}/identity-photos/{photoId}")
    public ApiResponse<Void> delete(@PathVariable Long petId, @PathVariable Long photoId) {
        petIdentityService.delete(petId, photoId);
        return ApiResponse.success();
    }

    @PostMapping(path = "/identity/match", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PetIdentityMatchResponse> match(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(petIdentityService.match(file));
    }
}
