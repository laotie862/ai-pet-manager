package com.example.demo.behavior;

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

@Repository
public class BehaviorSampleRepository {
    private static final RowMapper<BehaviorSampleRecord> SAMPLE_MAPPER = new BehaviorSampleRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public BehaviorSampleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createSample(
            Long petId,
            Long deviceId,
            Long eventId,
            String imagePath,
            String predictedBehavior,
            double confidence,
            boolean found,
            String provider,
            String modelVersion,
            String reviewStatus,
            String finalBehavior,
            OffsetDateTime capturedAt
    ) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO t_behavior_sample(
                    pet_id, device_id, event_id, image_path, predicted_behavior,
                    confidence, found, provider, model_version, review_status,
                    final_behavior, captured_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                petId,
                deviceId,
                eventId,
                imagePath,
                predictedBehavior,
                confidence,
                found,
                provider,
                modelVersion,
                reviewStatus,
                finalBehavior,
                Timestamp.from(capturedAt.toInstant())
        );
    }

    public List<BehaviorSampleRecord> listByPet(Long petId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM t_behavior_sample
                WHERE pet_id = ?
                ORDER BY captured_at DESC
                LIMIT ?
                """, SAMPLE_MAPPER, petId, Math.max(1, Math.min(200, limit)));
    }

    public List<BehaviorSampleRecord> listPendingReview(int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM t_behavior_sample
                WHERE review_status = 'pending_review'
                ORDER BY captured_at DESC
                LIMIT ?
                """, SAMPLE_MAPPER, Math.max(1, Math.min(200, limit)));
    }

    public Optional<BehaviorSampleRecord> findByIdAndPet(Long sampleId, Long petId) {
        List<BehaviorSampleRecord> samples = jdbcTemplate.query("""
                SELECT *
                FROM t_behavior_sample
                WHERE id = ? AND pet_id = ?
                """, SAMPLE_MAPPER, sampleId, petId);
        return samples.stream().findFirst();
    }

    public int reviewSample(Long sampleId, Long petId, String finalBehavior, OffsetDateTime reviewedAt) {
        return jdbcTemplate.update("""
                UPDATE t_behavior_sample
                SET review_status = 'reviewed',
                    final_behavior = ?,
                    reviewed_at = ?
                WHERE id = ? AND pet_id = ?
                """,
                finalBehavior,
                Timestamp.from(reviewedAt.toInstant()),
                sampleId,
                petId
        );
    }

    private static class BehaviorSampleRowMapper implements RowMapper<BehaviorSampleRecord> {
        @Override
        public BehaviorSampleRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BehaviorSampleRecord(
                    rs.getLong("id"),
                    rs.getLong("pet_id"),
                    rs.getLong("device_id"),
                    nullableLong(rs, "event_id"),
                    rs.getString("image_path"),
                    rs.getString("predicted_behavior"),
                    rs.getDouble("confidence"),
                    rs.getBoolean("found"),
                    rs.getString("provider"),
                    rs.getString("model_version"),
                    rs.getString("review_status"),
                    rs.getString("final_behavior"),
                    offsetDateTime(rs, "captured_at"),
                    offsetDateTime(rs, "created_at"),
                    offsetDateTime(rs, "reviewed_at")
            );
        }

        private static Long nullableLong(ResultSet rs, String column) throws SQLException {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        }

        private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            if (timestamp == null) {
                return null;
            }
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
    }
}
