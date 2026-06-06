package com.example.demo.user;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.SecurityUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse me() {
        Long userId = SecurityUtils.currentUser().id();
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserResponse updateMe(UpdateProfileRequest request) {
        Long userId = SecurityUtils.currentUser().id();
        try {
            return UserResponse.from(userRepository.updateProfile(userId, request.nickname(), request.phone()));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "Phone already exists");
        }
    }
}
