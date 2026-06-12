package com.example.demo.device;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.pet.PetResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ApiResponse<List<DeviceResponse>> list() {
        return ApiResponse.success(deviceService.list());
    }

    @PostMapping
    public ApiResponse<DeviceResponse> create(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResponse.success(deviceService.create(request));
    }

    @PostMapping("/test")
    public ApiResponse<DeviceStatusResponse> testConnection(@Valid @RequestBody DeviceConnectionTestRequest request) {
        return ApiResponse.success(deviceService.testConnection(request));
    }

    @GetMapping("/{deviceId}")
    public ApiResponse<DeviceResponse> detail(@PathVariable Long deviceId) {
        return ApiResponse.success(deviceService.detail(deviceId));
    }

    @GetMapping("/{deviceId}/pets")
    public ApiResponse<List<PetResponse>> listBoundPets(@PathVariable Long deviceId) {
        return ApiResponse.success(deviceService.listBoundPets(deviceId));
    }

    @PutMapping("/{deviceId}/pets")
    public ApiResponse<List<PetResponse>> replaceBoundPets(
            @PathVariable Long deviceId,
            @Valid @RequestBody DevicePetBindingRequest request
    ) {
        return ApiResponse.success(deviceService.replaceBoundPets(deviceId, request));
    }

    @PostMapping("/{deviceId}/pets/{petId}")
    public ApiResponse<List<PetResponse>> addBoundPet(@PathVariable Long deviceId, @PathVariable Long petId) {
        return ApiResponse.success(deviceService.addBoundPet(deviceId, petId));
    }

    @DeleteMapping("/{deviceId}/pets/{petId}")
    public ApiResponse<List<PetResponse>> removeBoundPet(@PathVariable Long deviceId, @PathVariable Long petId) {
        return ApiResponse.success(deviceService.removeBoundPet(deviceId, petId));
    }

    @GetMapping("/{deviceId}/status")
    public ApiResponse<DeviceStatusResponse> status(@PathVariable Long deviceId) {
        return ApiResponse.success(deviceService.status(deviceId));
    }

    @PostMapping("/{deviceId}/test")
    public ApiResponse<DeviceStatusResponse> testStoredConnection(@PathVariable Long deviceId) {
        return ApiResponse.success(deviceService.testStoredConnection(deviceId));
    }

    @PostMapping("/{deviceId}/stream/start")
    public ApiResponse<DeviceStatusResponse> startStream(@PathVariable Long deviceId) {
        return ApiResponse.success(deviceService.startStream(deviceId));
    }

    @PostMapping("/{deviceId}/stream/stop")
    public ApiResponse<DeviceStatusResponse> stopStream(@PathVariable Long deviceId) {
        return ApiResponse.success(deviceService.stopStream(deviceId));
    }

    @PutMapping("/{deviceId}/roi")
    public ApiResponse<DeviceResponse> updateRoi(
            @PathVariable Long deviceId,
            @Valid @RequestBody DeviceRoiRequest request
    ) {
        return ApiResponse.success(deviceService.updateRoi(deviceId, request));
    }

    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> delete(@PathVariable Long deviceId) {
        deviceService.delete(deviceId);
        return ApiResponse.success();
    }
}
