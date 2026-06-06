package com.example.demo.storage;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ObjectStorageService {
    private static final Logger log = LoggerFactory.getLogger(ObjectStorageService.class);
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final StorageProperties properties;
    private final MinioClient minioClient;
    private volatile boolean bucketReady;

    public ObjectStorageService(StorageProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    public StoredObject uploadPetPhoto(Long userId, Long petId, MultipartFile file) {
        validateFile(file);
        String contentType = resolveContentType(file);
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        String objectName = buildPetPhotoObjectName(userId, petId, extension);

        try (InputStream inputStream = file.getInputStream()) {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.normalizedBucket())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            return new StoredObject(objectName, properties.normalizedPublicBaseUrl() + "/" + objectName);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to upload pet photo to object storage", exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Pet photo upload failed");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Photo file is required");
        }
        if (file.getSize() > properties.getMaxUploadBytes()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Photo file is too large");
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null) {
            contentType = normalizeContentType(URLConnection.guessContentTypeFromName(file.getOriginalFilename()));
        }
        if (contentType == null || !EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only JPG, PNG, WebP, and GIF images are supported");
        }
        return contentType;
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int parameterStart = normalized.indexOf(';');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart).trim();
        }
        if (MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private String buildPetPhotoObjectName(Long userId, Long petId, String extension) {
        String fileName = UUID.randomUUID() + extension;
        String prefix = properties.normalizedPathPrefix();
        String ownerPath = "users/" + userId + "/pets/" + petId + "/" + fileName;
        return StringUtils.hasText(prefix) ? prefix + "/" + ownerPath : ownerPath;
    }

    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            String bucket = properties.normalizedBucket();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            if (properties.isPublicRead()) {
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(publicReadPolicy(bucket))
                        .build());
            }
            bucketReady = true;
        }
    }

    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }
}
