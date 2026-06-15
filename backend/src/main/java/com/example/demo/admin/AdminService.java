package com.example.demo.admin;

import com.example.demo.behavior.BehaviorEventRecord;
import com.example.demo.behavior.BehaviorRepository;
import com.example.demo.behavior.BehaviorSampleRepository;
import com.example.demo.auth.RefreshTokenRepository;
import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.api.PageResponse;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.device.DeviceRepository;
import com.example.demo.device.DeviceResponse;
import com.example.demo.device.DeviceRecord;
import com.example.demo.device.DeviceStreamManager;
import com.example.demo.device.DeviceStreamSnapshot;
import com.example.demo.device.DeviceStatusResponse;
import com.example.demo.pet.PetRepository;
import com.example.demo.pet.PetResponse;
import com.example.demo.user.UserAccount;
import com.example.demo.user.UserRepository;
import com.example.demo.user.UserResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStreamManager streamManager;
    private final BehaviorRepository behaviorRepository;
    private final BehaviorSampleRepository sampleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminService(UserRepository userRepository,
                        PetRepository petRepository,
                        DeviceRepository deviceRepository,
                        DeviceStreamManager streamManager,
                        BehaviorRepository behaviorRepository,
                        BehaviorSampleRepository sampleRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.deviceRepository = deviceRepository;
        this.streamManager = streamManager;
        this.behaviorRepository = behaviorRepository;
        this.sampleRepository = sampleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // --- Users ---

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
        refreshTokenRepository.revokeAllForUser(userId);
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

    // --- Stats ---

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("userCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user WHERE status = 'ACTIVE'", Long.class));

        stats.put("petCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_pet", Long.class));

        stats.put("deviceCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_device", Long.class));

        stats.put("deviceOnlineCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_device WHERE status IN ('ONLINE','ANALYZING')", Long.class));

        // Today's behavior event count
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        stats.put("todayEventCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_behavior_event WHERE started_at >= ?",
                Long.class,
                Timestamp.from(today.atStartOfDay().toInstant(ZoneOffset.UTC))));

        // Sample counts
        stats.put("pendingSampleCount", sampleRepository.countByStatus("PENDING"));
        stats.put("autoApprovedSampleCount", sampleRepository.countByStatus("AUTO_APPROVED"));
        stats.put("confirmedSampleCount", sampleRepository.countByStatus("CONFIRMED"));

        return stats;
    }

    // --- Pets ---

    public PageResponse<PetResponse> pets(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<PetResponse> items = petRepository.listAll(safeSize, safePage * safeSize)
                .stream()
                .map(PetResponse::from)
                .toList();
        int total = petRepository.countAll();
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    @Transactional
    public void deletePet(Long petId) {
        requirePet(petId);
        jdbcTemplate.update("DELETE FROM t_device_pet WHERE pet_id = ?", petId);
        jdbcTemplate.update("UPDATE t_device SET pet_id = NULL, updated_at = CURRENT_TIMESTAMP WHERE pet_id = ?", petId);
        jdbcTemplate.update("DELETE FROM t_behavior_sample WHERE pet_id = ?", petId);
        jdbcTemplate.update("DELETE FROM t_behavior_summary WHERE pet_id = ?", petId);
        jdbcTemplate.update("DELETE FROM t_behavior_event WHERE pet_id = ?", petId);
        jdbcTemplate.update("DELETE FROM t_alert WHERE pet_id = ?", petId);
        jdbcTemplate.update("DELETE FROM t_pet_identity_photo WHERE pet_id = ?", petId);
        int deleted = jdbcTemplate.update("DELETE FROM t_pet WHERE id = ?", petId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Pet not found");
        }
        audit("DELETE_PET", "PET", petId);
    }

    // --- Devices ---

    public PageResponse<DeviceResponse> devices(String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<DeviceResponse> items = deviceRepository.listAll(status, safeSize, safePage * safeSize)
                .stream()
                .map(device -> DeviceResponse.from(device, List.of()))
                .toList();
        int total = deviceRepository.countAll(status);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    @Transactional
    public DeviceStatusResponse startDevice(Long deviceId) {
        DeviceRecord device = requireDevice(deviceId);
        DeviceStreamSnapshot snapshot = streamManager.start(device);
        audit("START_DEVICE", "DEVICE", deviceId);
        DeviceRecord updated = deviceRepository.findById(deviceId).orElse(device);
        return DeviceStatusResponse.from(updated, snapshot);
    }

    @Transactional
    public DeviceStatusResponse stopDevice(Long deviceId) {
        DeviceRecord device = requireDevice(deviceId);
        streamManager.stop(device.id());
        audit("STOP_DEVICE", "DEVICE", deviceId);
        DeviceRecord updated = deviceRepository.findById(deviceId).orElse(device);
        return DeviceStatusResponse.from(updated, streamManager.snapshot(deviceId));
    }

    @Transactional
    public void deleteDevice(Long deviceId) {
        DeviceRecord device = requireDevice(deviceId);
        streamManager.stop(device.id());
        jdbcTemplate.update("DELETE FROM t_device_pet WHERE device_id = ?", deviceId);
        jdbcTemplate.update("DELETE FROM t_behavior_sample WHERE device_id = ?", deviceId);
        jdbcTemplate.update("UPDATE t_behavior_event SET device_id = NULL WHERE device_id = ?", deviceId);
        int deleted = jdbcTemplate.update("DELETE FROM t_device WHERE id = ?", deviceId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Device not found");
        }
        audit("DELETE_DEVICE", "DEVICE", deviceId);
    }

    // --- Behaviors ---

    public PageResponse<BehaviorEventRecord> behaviors(
            Long petId, LocalDate date, String behaviorType, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<BehaviorEventRecord> items = behaviorRepository.listAll(
                petId, date, behaviorType, safeSize, safePage * safeSize);
        int total = behaviorRepository.countAll(petId, date, behaviorType);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    // --- Audit ---

    public PageResponse<Map<String, Object>> auditLog(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        List<Map<String, Object>> items = jdbcTemplate.query(
                "SELECT a.id, a.action, a.target_type, a.target_id, a.created_at, " +
                "COALESCE(u.nickname, u.email, 'System') AS admin_name " +
                "FROM t_admin_audit a " +
                "LEFT JOIN t_user u ON a.admin_user_id = u.id " +
                "ORDER BY a.created_at DESC " +
                "LIMIT ? OFFSET ?",
                (rs, rowNum) -> java.util.Map.of(
                        "id", rs.getLong("id"),
                        "action", rs.getString("action"),
                        "targetType", rs.getString("target_type"),
                        "targetId", rs.getString("target_id"),
                        "adminName", rs.getString("admin_name"),
                        "createdAt", rs.getTimestamp("created_at").toInstant().toString()
                ),
                safeSize, safePage * safeSize
        );

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_admin_audit", Integer.class);
        return new PageResponse<>(items, total != null ? total : 0, safePage, safeSize);
    }

    // --- Internal helpers ---

    private UserAccount requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
    }

    private void requirePet(Long petId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_pet WHERE id = ?", Integer.class, petId);
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Pet not found");
        }
    }

    private DeviceRecord requireDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Device not found"));
    }

    private void audit(String action, String targetType, Long targetId) {
        Long adminUserId = SecurityUtils.currentUser().id();
        jdbcTemplate.update("""
                INSERT INTO t_admin_audit(admin_user_id, action, target_type, target_id)
                VALUES (?, ?, ?, ?)
                """, adminUserId, action, targetType, String.valueOf(targetId));
    }
}
