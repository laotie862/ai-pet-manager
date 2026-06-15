package com.example.demo.admin;

import com.example.demo.behavior.BehaviorEventRecord;
import com.example.demo.behavior.BehaviorSampleRecord;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.common.api.PageResponse;
import com.example.demo.device.DeviceResponse;
import com.example.demo.pet.PetResponse;
import com.example.demo.user.UserResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // --- Users ---

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.users(keyword, page, size));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserResponse> user(@PathVariable Long userId) {
        return ApiResponse.success(adminService.user(userId));
    }

    @PostMapping("/users/{userId}/ban")
    public ApiResponse<UserResponse> ban(@PathVariable Long userId) {
        return ApiResponse.success(adminService.ban(userId));
    }

    @PostMapping("/users/{userId}/unban")
    public ApiResponse<UserResponse> unban(@PathVariable Long userId) {
        return ApiResponse.success(adminService.unban(userId));
    }

    // --- Stats ---

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(adminService.stats());
    }

    // --- Pets ---

    @GetMapping("/pets")
    public ApiResponse<PageResponse<PetResponse>> pets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.pets(page, size));
    }

    @DeleteMapping("/pets/{petId}")
    public ApiResponse<Void> deletePet(@PathVariable Long petId) {
        adminService.deletePet(petId);
        return ApiResponse.success();
    }

    // --- Devices ---

    @GetMapping("/devices")
    public ApiResponse<PageResponse<DeviceResponse>> devices(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.devices(status, page, size));
    }

    @PostMapping("/devices/{deviceId}/stream/start")
    public ApiResponse<?> startDevice(@PathVariable Long deviceId) {
        return ApiResponse.success(adminService.startDevice(deviceId));
    }

    @PostMapping("/devices/{deviceId}/stream/stop")
    public ApiResponse<?> stopDevice(@PathVariable Long deviceId) {
        return ApiResponse.success(adminService.stopDevice(deviceId));
    }

    @DeleteMapping("/devices/{deviceId}")
    public ApiResponse<Void> deleteDevice(@PathVariable Long deviceId) {
        adminService.deleteDevice(deviceId);
        return ApiResponse.success();
    }

    // --- Behaviors ---

    @GetMapping("/behaviors")
    public ApiResponse<PageResponse<BehaviorEventRecord>> behaviors(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false) String behaviorType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.behaviors(petId, date, behaviorType, page, size));
    }

    // --- Audit ---

    @GetMapping("/audit")
    public ApiResponse<PageResponse<Map<String, Object>>> audit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.auditLog(page, size));
    }
}
