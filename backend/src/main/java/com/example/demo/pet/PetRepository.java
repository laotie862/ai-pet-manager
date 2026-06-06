package com.example.demo.pet;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class PetRepository {
    private static final RowMapper<PetRecord> PET_MAPPER = new PetRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public PetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(Long userId, PetRequest request) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO t_pet(user_id, name, species, breed, gender, birthday, avatar_url, weight_kg)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                userId,
                request.name().trim(),
                request.species().trim(),
                blankToNull(request.breed()),
                blankToNull(request.gender()),
                request.birthday(),
                blankToNull(request.avatarUrl()),
                request.weightKg()
        );
    }

    public List<PetRecord> listByUser(Long userId) {
        return jdbcTemplate.query(
                "SELECT * FROM t_pet WHERE user_id = ? ORDER BY created_at DESC",
                PET_MAPPER,
                userId
        );
    }

    public long countByUser(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM t_pet WHERE user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public Optional<PetRecord> findByIdAndUser(Long petId, Long userId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM t_pet WHERE id = ? AND user_id = ?",
                    PET_MAPPER,
                    petId,
                    userId
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public PetRecord update(Long petId, Long userId, PetRequest request) {
        jdbcTemplate.update("""
                UPDATE t_pet
                SET name = ?,
                    species = ?,
                    breed = ?,
                    gender = ?,
                    birthday = ?,
                    avatar_url = ?,
                    weight_kg = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """,
                request.name().trim(),
                request.species().trim(),
                blankToNull(request.breed()),
                blankToNull(request.gender()),
                request.birthday(),
                blankToNull(request.avatarUrl()),
                request.weightKg(),
                petId,
                userId
        );
        return findByIdAndUser(petId, userId).orElseThrow();
    }

    public Optional<PetRecord> updateAvatarUrl(Long petId, Long userId, String avatarUrl) {
        int updated = jdbcTemplate.update("""
                UPDATE t_pet
                SET avatar_url = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """,
                avatarUrl,
                petId,
                userId
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return findByIdAndUser(petId, userId);
    }

    public int delete(Long petId, Long userId) {
        return jdbcTemplate.update("DELETE FROM t_pet WHERE id = ? AND user_id = ?", petId, userId);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static class PetRowMapper implements RowMapper<PetRecord> {
        @Override
        public PetRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PetRecord(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("name"),
                    rs.getString("species"),
                    rs.getString("breed"),
                    rs.getString("gender"),
                    rs.getDate("birthday") == null ? null : rs.getDate("birthday").toLocalDate(),
                    rs.getString("avatar_url"),
                    rs.getBigDecimal("weight_kg"),
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
