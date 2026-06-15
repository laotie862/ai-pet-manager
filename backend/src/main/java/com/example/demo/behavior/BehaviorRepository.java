package com.example.demo.behavior;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class BehaviorRepository {
    private static final RowMapper<BehaviorEventRecord> EVENT_MAPPER = new BehaviorEventRowMapper();
    private static final RowMapper<BehaviorSummaryResponse> SUMMARY_MAPPER = new BehaviorSummaryRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public BehaviorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createEvent(
            Long petId,
            Long deviceId,
            String behaviorType,
            double confidence,
            boolean found,
            OffsetDateTime startedAt,
            String modelVersion
    ) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO t_behavior_event(
                    pet_id, device_id, behavior_type, confidence, found, started_at, model_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                petId,
                deviceId,
                behaviorType,
                confidence,
                found,
                Timestamp.from(startedAt.toInstant()),
                modelVersion
        );
    }

    public void completeEvent(Long eventId, OffsetDateTime endedAt) {
        jdbcTemplate.update(
                "UPDATE t_behavior_event SET ended_at = ? WHERE id = ? AND ended_at IS NULL",
                Timestamp.from(endedAt.toInstant()),
                eventId
        );
    }

    public Optional<BehaviorEventRecord> currentByDevice(Long deviceId) {
        return queryOne("""
                SELECT *
                FROM t_behavior_event
                WHERE device_id = ? AND ended_at IS NULL
                ORDER BY started_at DESC
                LIMIT 1
                """, deviceId);
    }

    public List<BehaviorEventRecord> openByDeviceAndPet(Long deviceId, Long petId) {
        return jdbcTemplate.query("""
                SELECT *
                FROM t_behavior_event
                WHERE device_id = ?
                  AND pet_id = ?
                  AND ended_at IS NULL
                ORDER BY started_at DESC
                """, EVENT_MAPPER, deviceId, petId);
    }

    public Optional<BehaviorEventRecord> currentByPet(Long petId) {
        return queryOne("""
                SELECT *
                FROM t_behavior_event
                WHERE pet_id = ? AND ended_at IS NULL
                ORDER BY started_at DESC
                LIMIT 1
                """, petId);
    }

    public List<BehaviorEventRecord> timeline(Long petId, LocalDate date) {
        return jdbcTemplate.query("""
                SELECT *
                FROM t_behavior_event
                WHERE pet_id = ?
                  AND started_at >= ?
                  AND started_at < ?
                ORDER BY started_at DESC
                """,
                EVENT_MAPPER,
                petId,
                Timestamp.from(date.atStartOfDay().toInstant(ZoneOffset.UTC)),
                Timestamp.from(date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
        );
    }

    public Optional<BehaviorSummaryResponse> summary(Long petId, LocalDate date) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM t_behavior_summary WHERE pet_id = ? AND summary_date = ?",
                    SUMMARY_MAPPER,
                    petId,
                    Date.valueOf(date)
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public void accumulateSummary(Long petId, LocalDate date, String behaviorType, long durationSeconds) {
        int eating = "eating".equals(behaviorType) ? 1 : 0;
        int drinking = "drinking".equals(behaviorType) ? 1 : 0;
        int exercising = "exercising".equals(behaviorType) ? Math.toIntExact(Math.max(0, durationSeconds)) : 0;
        int sleeping = "sleeping".equals(behaviorType) ? Math.toIntExact(Math.max(0, durationSeconds)) : 0;
        int defecating = "defecating".equals(behaviorType) ? 1 : 0;

        jdbcTemplate.update("""
                INSERT INTO t_behavior_summary(
                    pet_id, summary_date, eating_count, drinking_count,
                    exercising_seconds, sleeping_seconds, defecating_count
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (pet_id, summary_date)
                DO UPDATE SET
                    eating_count = t_behavior_summary.eating_count + EXCLUDED.eating_count,
                    drinking_count = t_behavior_summary.drinking_count + EXCLUDED.drinking_count,
                    exercising_seconds = t_behavior_summary.exercising_seconds + EXCLUDED.exercising_seconds,
                    sleeping_seconds = t_behavior_summary.sleeping_seconds + EXCLUDED.sleeping_seconds,
                    defecating_count = t_behavior_summary.defecating_count + EXCLUDED.defecating_count,
                    updated_at = CURRENT_TIMESTAMP
                """,
                petId,
                Date.valueOf(date),
                eating,
                drinking,
                exercising,
                sleeping,
                defecating
        );
    }

    // Admin: list all behavior events, optionally filtered
    public List<BehaviorEventRecord> listAll(
            Long petId, LocalDate date, String behaviorType, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM t_behavior_event WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (petId != null) {
            sql.append(" AND pet_id = ?");
            params.add(petId);
        }
        if (date != null) {
            sql.append(" AND started_at >= ? AND started_at < ?");
            params.add(Timestamp.from(date.atStartOfDay().toInstant(ZoneOffset.UTC)));
            params.add(Timestamp.from(date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
        }
        if (behaviorType != null && !behaviorType.isBlank()) {
            sql.append(" AND behavior_type = ?");
            params.add(behaviorType);
        }

        sql.append(" ORDER BY started_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), EVENT_MAPPER, params.toArray());
    }

    public int countAll(Long petId, LocalDate date, String behaviorType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM t_behavior_event WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (petId != null) {
            sql.append(" AND pet_id = ?");
            params.add(petId);
        }
        if (date != null) {
            sql.append(" AND started_at >= ? AND started_at < ?");
            params.add(Timestamp.from(date.atStartOfDay().toInstant(ZoneOffset.UTC)));
            params.add(Timestamp.from(date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
        }
        if (behaviorType != null && !behaviorType.isBlank()) {
            sql.append(" AND behavior_type = ?");
            params.add(behaviorType);
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    private Optional<BehaviorEventRecord> queryOne(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, EVENT_MAPPER, args));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private static class BehaviorEventRowMapper implements RowMapper<BehaviorEventRecord> {
        @Override
        public BehaviorEventRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BehaviorEventRecord(
                    rs.getLong("id"),
                    rs.getLong("pet_id"),
                    rs.getLong("device_id"),
                    rs.getString("behavior_type"),
                    rs.getDouble("confidence"),
                    rs.getBoolean("found"),
                    offsetDateTime(rs, "started_at"),
                    offsetDateTime(rs, "ended_at"),
                    rs.getString("model_version"),
                    offsetDateTime(rs, "created_at")
            );
        }
    }

    private static class BehaviorSummaryRowMapper implements RowMapper<BehaviorSummaryResponse> {
        @Override
        public BehaviorSummaryResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BehaviorSummaryResponse(
                    rs.getLong("pet_id"),
                    rs.getDate("summary_date").toLocalDate(),
                    rs.getInt("eating_count"),
                    rs.getInt("drinking_count"),
                    rs.getInt("exercising_seconds"),
                    rs.getInt("sleeping_seconds"),
                    rs.getInt("defecating_count"),
                    offsetDateTime(rs, "updated_at")
            );
        }
    }

    private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
