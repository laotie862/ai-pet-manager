package com.example.demo.pet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class PetIdentityRepository {
    private static final RowMapper<PetIdentityPhotoRecord> PHOTO_MAPPER = new PhotoRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public PetIdentityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(Long petId, String objectName, String imageUrl, String embeddingJson, String modelVersion) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO t_pet_identity_photo(pet_id, object_name, image_url, embedding_json, model_version)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, petId, objectName, imageUrl, embeddingJson, modelVersion);
    }

    public List<PetIdentityPhotoRecord> listByPet(Long petId) {
        return jdbcTemplate.query(
                "SELECT * FROM t_pet_identity_photo WHERE pet_id = ? ORDER BY created_at DESC",
                PHOTO_MAPPER,
                petId
        );
    }

    public List<PetIdentityPhotoRecord> listByUser(Long userId) {
        return jdbcTemplate.query("""
                SELECT p.*
                FROM t_pet_identity_photo p
                JOIN t_pet pet ON pet.id = p.pet_id
                WHERE pet.user_id = ?
                ORDER BY p.created_at DESC
                """, PHOTO_MAPPER, userId);
    }

    public int delete(Long petId, Long photoId) {
        return jdbcTemplate.update(
                "DELETE FROM t_pet_identity_photo WHERE pet_id = ? AND id = ?",
                petId,
                photoId
        );
    }

    private static class PhotoRowMapper implements RowMapper<PetIdentityPhotoRecord> {
        @Override
        public PetIdentityPhotoRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PetIdentityPhotoRecord(
                    rs.getLong("id"),
                    rs.getLong("pet_id"),
                    rs.getString("object_name"),
                    rs.getString("image_url"),
                    rs.getString("embedding_json"),
                    rs.getString("model_version"),
                    offsetDateTime(rs, "created_at")
            );
        }

        private OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(column);
            return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
    }
}
