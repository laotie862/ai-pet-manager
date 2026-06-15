package com.example.demo.behavior;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
public class BehaviorSampleRepository {
    private static final RowMapper<BehaviorSampleRecord> MAPPER = new SampleRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public BehaviorSampleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BehaviorSampleRecord create(
            Long petId, Long deviceId, String behaviorType,
            double confidence, String imagePath, String reviewStatus,
            String modelVersion, OffsetDateTime capturedAt
    ) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO t_behavior_sample(
                    pet_id, device_id, behavior_type, confidence,
                    image_path, review_status, model_version, captured_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, pet_id, device_id, behavior_type, confidence,
                          image_path, review_status, final_behavior, model_version,
                          captured_at, reviewed_at, created_at
                """, MAPPER,
                petId, deviceId, behaviorType,
                confidence, imagePath, reviewStatus,
                modelVersion, Timestamp.from(capturedAt.toInstant())
        );
    }

    public List<BehaviorSampleRecord> list(
            String status, String behaviorType, Integer page, Integer size
    ) {
        int safePage = Math.max(page != null ? page : 0, 0);
        int safeSize = Math.min(Math.max(size != null ? size : 20, 1), 100);

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM t_behavior_sample
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND review_status = ?");
            params.add(status);
        }
        if (behaviorType != null && !behaviorType.isBlank()) {
            sql.append(" AND behavior_type = ?");
            params.add(behaviorType);
        }

        sql.append(" ORDER BY captured_at DESC LIMIT ? OFFSET ?");
        params.add(safeSize);
        params.add((long) safePage * safeSize);

        return jdbcTemplate.query(sql.toString(), MAPPER, params.toArray());
    }

    public int count(String status, String behaviorType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM t_behavior_sample WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND review_status = ?");
            params.add(status);
        }
        if (behaviorType != null && !behaviorType.isBlank()) {
            sql.append(" AND behavior_type = ?");
            params.add(behaviorType);
        }

        Integer result = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return result != null ? result : 0;
    }

    public int countByStatus(String status) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_behavior_sample WHERE review_status = ?",
                Integer.class, status
        );
        return result != null ? result : 0;
    }

    public java.util.Optional<BehaviorSampleRecord> findById(Long id) {
        try {
            return java.util.Optional.ofNullable(
                    jdbcTemplate.queryForObject(
                            "SELECT * FROM t_behavior_sample WHERE id = ?", MAPPER, id
                    )
            );
        } catch (EmptyResultDataAccessException e) {
            return java.util.Optional.empty();
        }
    }

    public void review(Long id, String finalBehavior, String reviewStatus, OffsetDateTime reviewedAt) {
        jdbcTemplate.update("""
                UPDATE t_behavior_sample
                SET final_behavior = ?,
                    review_status = ?,
                    reviewed_at = ?
                WHERE id = ?
                """, finalBehavior, reviewStatus, Timestamp.from(reviewedAt.toInstant()), id);
    }

    private static class SampleRowMapper implements RowMapper<BehaviorSampleRecord> {
        @Override
        public BehaviorSampleRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BehaviorSampleRecord(
                    rs.getLong("id"),
                    rs.getLong("pet_id"),
                    rs.getLong("device_id"),
                    rs.getString("behavior_type"),
                    rs.getDouble("confidence"),
                    rs.getString("image_path"),
                    rs.getString("review_status"),
                    rs.getString("final_behavior"),
                    rs.getString("model_version"),
                    offsetDateTime(rs, "captured_at"),
                    offsetDateTime(rs, "reviewed_at"),
                    offsetDateTime(rs, "created_at")
            );
        }

        private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
            Timestamp ts = rs.getTimestamp(column);
            return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
        }
    }
}
