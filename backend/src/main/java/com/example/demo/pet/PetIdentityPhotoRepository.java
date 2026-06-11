package com.example.demo.pet;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class PetIdentityPhotoRepository {
    private static final TypeReference<List<Double>> EMBEDDING_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RowMapper<PetIdentityPhotoRecord> rowMapper = new PetIdentityPhotoRowMapper();

    public PetIdentityPhotoRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public PetIdentityPhotoRecord create(
            Long petId,
            String objectName,
            String imageUrl,
            List<Double> embedding,
            String modelVersion
    ) {
        String embeddingJson = toJson(embedding);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO t_pet_identity_photo(pet_id, object_name, image_url, embedding_json, model_version)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, petId, objectName, imageUrl, embeddingJson, modelVersion);
        return findById(id);
    }

    public List<PetIdentityPhotoRecord> listByPet(Long petId) {
        return jdbcTemplate.query("""
                SELECT *
                FROM t_pet_identity_photo
                WHERE pet_id = ?
                ORDER BY created_at DESC
                """, rowMapper, petId);
    }

    public List<PetIdentityPhotoRecord> listByUser(Long userId) {
        return jdbcTemplate.query("""
                SELECT pip.*
                FROM t_pet_identity_photo pip
                JOIN t_pet p ON p.id = pip.pet_id
                WHERE p.user_id = ?
                ORDER BY pip.created_at DESC
                """, rowMapper, userId);
    }

    public int delete(Long photoId, Long petId) {
        return jdbcTemplate.update(
                "DELETE FROM t_pet_identity_photo WHERE id = ? AND pet_id = ?",
                photoId,
                petId
        );
    }

    private PetIdentityPhotoRecord findById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM t_pet_identity_photo WHERE id = ?",
                rowMapper,
                id
        );
    }

    private String toJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Embedding serialization failed");
        }
    }

    private List<Double> fromJson(String embeddingJson) {
        try {
            return objectMapper.readValue(embeddingJson, EMBEDDING_TYPE);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Embedding data is invalid");
        }
    }

    private class PetIdentityPhotoRowMapper implements RowMapper<PetIdentityPhotoRecord> {
        @Override
        public PetIdentityPhotoRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PetIdentityPhotoRecord(
                    rs.getLong("id"),
                    rs.getLong("pet_id"),
                    rs.getString("object_name"),
                    rs.getString("image_url"),
                    fromJson(rs.getString("embedding_json")),
                    rs.getString("model_version"),
                    offsetDateTime(rs.getTimestamp("created_at"))
            );
        }

        private OffsetDateTime offsetDateTime(Timestamp timestamp) {
            if (timestamp == null) {
                return null;
            }
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
    }
}
