package com.example.demo.device;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceRepository {
    private static final RowMapper<DeviceRecord> DEVICE_MAPPER = new DeviceRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public DeviceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(
            Long userId,
            Long petId,
            String name,
            String rtspUrl,
            String rtspUsername,
            String rtspPasswordCipher
    ) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO t_device(
                    user_id, pet_id, name, rtsp_url, rtsp_username,
                    rtsp_password_cipher, stream_key, status, last_online_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class,
                userId,
                petId,
                name,
                rtspUrl,
                rtspUsername,
                rtspPasswordCipher,
                UUID.randomUUID().toString(),
                DeviceStatus.ONLINE.name()
        );
    }

    public List<DeviceRecord> listByUser(Long userId) {
        return jdbcTemplate.query(
                "SELECT * FROM t_device WHERE user_id = ? ORDER BY created_at DESC",
                DEVICE_MAPPER,
                userId
        );
    }

    public List<DeviceRecord> listAssignedDevices() {
        return jdbcTemplate.query(
                "SELECT * FROM t_device WHERE pet_id IS NOT NULL ORDER BY id",
                DEVICE_MAPPER
        );
    }

    public Optional<DeviceRecord> findByIdAndUser(Long deviceId, Long userId) {
        return queryOne("SELECT * FROM t_device WHERE id = ? AND user_id = ?", deviceId, userId);
    }

    public Optional<DeviceRecord> findById(Long deviceId) {
        return queryOne("SELECT * FROM t_device WHERE id = ?", deviceId);
    }

    public long countByUser(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM t_device WHERE user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public Optional<DeviceRecord> updateRoi(Long deviceId, Long userId, String roiJson) {
        int updated = jdbcTemplate.update("""
                UPDATE t_device
                SET roi_polygon = ?::jsonb,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """,
                roiJson,
                deviceId,
                userId
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return findByIdAndUser(deviceId, userId);
    }

    public void updateRuntimeState(Long deviceId, DeviceStatus status, String lastError, boolean heartbeat) {
        jdbcTemplate.update("""
                UPDATE t_device
                SET status = ?,
                    last_error = ?,
                    last_online_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE last_online_at END,
                    last_heartbeat_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE last_heartbeat_at END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                status.name(),
                lastError,
                status != DeviceStatus.OFFLINE,
                heartbeat,
                deviceId
        );
    }

    public void touchHeartbeat(Long deviceId) {
        jdbcTemplate.update("""
                UPDATE t_device
                SET status = ?,
                    last_online_at = CURRENT_TIMESTAMP,
                    last_heartbeat_at = CURRENT_TIMESTAMP,
                    last_error = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                DeviceStatus.ANALYZING.name(),
                deviceId
        );
    }

    public int delete(Long deviceId, Long userId) {
        return jdbcTemplate.update("DELETE FROM t_device WHERE id = ? AND user_id = ?", deviceId, userId);
    }

    private Optional<DeviceRecord> queryOne(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, DEVICE_MAPPER, args));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private static class DeviceRowMapper implements RowMapper<DeviceRecord> {
        @Override
        public DeviceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long petId = rs.getLong("pet_id");
            if (rs.wasNull()) {
                petId = null;
            }
            return new DeviceRecord(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    petId,
                    rs.getString("name"),
                    rs.getString("rtsp_url"),
                    rs.getString("rtsp_username"),
                    rs.getString("rtsp_password_cipher"),
                    rs.getString("stream_key"),
                    DeviceStatus.from(rs.getString("status")),
                    rs.getString("roi_polygon"),
                    offsetDateTime(rs, "last_online_at"),
                    offsetDateTime(rs, "last_heartbeat_at"),
                    rs.getString("last_error"),
                    offsetDateTime(rs, "created_at"),
                    offsetDateTime(rs, "updated_at")
            );
        }

        private OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            if (timestamp == null) {
                return null;
            }
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
    }
}
