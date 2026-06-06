package com.example.demo.admin;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.api.PageResponse;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.user.UserAccount;
import com.example.demo.user.UserRepository;
import com.example.demo.user.UserResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminService(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<UserResponse> users(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<UserResponse> items = userRepository.list(keyword, safeSize, safePage * safeSize)
                .stream()
                .map(UserResponse::from)
                .toList();
        return new PageResponse<>(items, userRepository.count(keyword), safePage, safeSize);
    }

    public UserResponse user(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserResponse ban(Long userId) {
        UserAccount target = requireUser(userId);
        if ("ADMIN".equals(target.role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Admin users cannot be banned");
        }
        userRepository.updateStatus(userId, "BANNED");
        audit("BAN_USER", "USER", userId);
        return user(userId);
    }

    @Transactional
    public UserResponse unban(Long userId) {
        requireUser(userId);
        userRepository.updateStatus(userId, "ACTIVE");
        audit("UNBAN_USER", "USER", userId);
        return user(userId);
    }

    private UserAccount requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
    }

    private void audit(String action, String targetType, Long targetId) {
        Long adminUserId = SecurityUtils.currentUser().id();
        jdbcTemplate.update("""
                INSERT INTO t_admin_audit(admin_user_id, action, target_type, target_id)
                VALUES (?, ?, ?, ?)
                """, adminUserId, action, targetType, String.valueOf(targetId));
    }
}
