package com.example.demo.admin;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.common.api.PageResponse;
import com.example.demo.user.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

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
}
