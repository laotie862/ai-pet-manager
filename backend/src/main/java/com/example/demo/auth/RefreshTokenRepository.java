package com.example.demo.auth;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {
    private static final RowMapper<RefreshTokenRecord> TOKEN_MAPPER = new RefreshTokenRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(Long userId, String tokenHash, OffsetDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO t_refresh_token(user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """, userId, tokenHash, Timestamp.from(expiresAt.toInstant()));
    }

    public Optional<RefreshTokenRecord> findByHash(String tokenHash) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM t_refresh_token WHERE token_hash = ?",
                    TOKEN_MAPPER,
                    tokenHash
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public void revoke(String tokenHash) {
        jdbcTemplate.update(
                "UPDATE t_refresh_token SET revoked_at = CURRENT_TIMESTAMP WHERE token_hash = ? AND revoked_at IS NULL",
                tokenHash
        );
    }

    public void revokeAllForUser(Long userId) {
        jdbcTemplate.update(
                "UPDATE t_refresh_token SET revoked_at = CURRENT_TIMESTAMP WHERE user_id = ? AND revoked_at IS NULL",
                userId
        );
    }

    private static class RefreshTokenRowMapper implements RowMapper<RefreshTokenRecord> {
        @Override
        public RefreshTokenRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RefreshTokenRecord(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getString("token_hash"),
                    offsetDateTime(rs, "expires_at"),
                    offsetDateTime(rs, "revoked_at")
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
