package com.example.demo.user;

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
public class UserRepository {
    private static final RowMapper<UserAccount> USER_MAPPER = new UserRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(UserCreateCommand command) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO t_user(email, phone, password_hash, nickname, role, status)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                normalized(command.email()),
                blankToNull(command.phone()),
                command.passwordHash(),
                command.nickname(),
                command.role(),
                command.status()
        );
    }

    public Optional<UserAccount> findById(Long id) {
        return queryOne("SELECT * FROM t_user WHERE id = ?", id);
    }

    public Optional<UserAccount> findByAccount(String account) {
        String normalizedAccount = normalized(account);
        return queryOne("""
                SELECT * FROM t_user
                WHERE lower(email) = ? OR phone = ?
                LIMIT 1
                """, normalizedAccount, account);
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM t_user WHERE lower(email) = ?",
                Integer.class,
                normalized(email)
        );
        return count != null && count > 0;
    }

    public boolean existsByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM t_user WHERE phone = ?",
                Integer.class,
                phone
        );
        return count != null && count > 0;
    }

    public UserAccount updateProfile(Long userId, String nickname, String phone) {
        jdbcTemplate.update("""
                UPDATE t_user
                SET nickname = COALESCE(?, nickname),
                    phone = COALESCE(?, phone),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, blankToNull(nickname), blankToNull(phone), userId);
        return findById(userId).orElseThrow();
    }

    public void updateStatus(Long userId, String status) {
        jdbcTemplate.update(
                "UPDATE t_user SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                status,
                userId
        );
    }

    public long count(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM t_user", Long.class);
            return count == null ? 0 : count;
        }

        String like = like(keyword);
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM t_user
                WHERE lower(coalesce(email, '')) LIKE ?
                    OR lower(coalesce(nickname, '')) LIKE ?
                    OR coalesce(phone, '') LIKE ?
                """, Long.class, like, like, like);
        return count == null ? 0 : count;
    }

    public List<UserAccount> list(String keyword, int size, int offset) {
        if (!StringUtils.hasText(keyword)) {
            return jdbcTemplate.query("""
                    SELECT *
                    FROM t_user
                    ORDER BY created_at DESC
                    LIMIT ? OFFSET ?
                    """, USER_MAPPER, size, offset);
        }

        String like = like(keyword);
        return jdbcTemplate.query("""
                SELECT *
                FROM t_user
                WHERE lower(coalesce(email, '')) LIKE ?
                    OR lower(coalesce(nickname, '')) LIKE ?
                    OR coalesce(phone, '') LIKE ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, USER_MAPPER, like, like, like, size, offset);
    }

    private Optional<UserAccount> queryOne(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, USER_MAPPER, args));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private String normalized(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String like(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    private static class UserRowMapper implements RowMapper<UserAccount> {
        @Override
        public UserAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new UserAccount(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("password_hash"),
                    rs.getString("nickname"),
                    rs.getString("role"),
                    rs.getString("status"),
                    offsetDateTime(rs, "vip_until"),
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
