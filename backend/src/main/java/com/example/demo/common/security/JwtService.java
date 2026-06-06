package com.example.demo.common.security;

import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityProperties securityProperties;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String createAccessToken(Long userId, String role) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(securityProperties.getAccessTokenMinutes() * 60);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", userId);
        payload.put("role", role);
        payload.put("typ", "access");
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        return createToken(payload);
    }

    public Optional<JwtClaims> parseAccessToken(String token) {
        try {
            JwtClaims claims = parse(token);
            if (!"access".equals(claims.tokenType())) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public Instant expiresAt(String token) {
        return parse(token).expiresAt();
    }

    private JwtClaims parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid token");
        }

        String signedContent = parts[0] + "." + parts[1];
        String expectedSignature = sign(signedContent);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid token signature");
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(base64UrlDecode(parts[1]), MAP_TYPE);
            Instant expiresAt = Instant.ofEpochSecond(asLong(payload.get("exp")));
            if (expiresAt.isBefore(Instant.now())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token expired");
            }
            return new JwtClaims(
                    asLong(payload.get("sub")),
                    String.valueOf(payload.get("role")),
                    String.valueOf(payload.get("typ")),
                    Instant.ofEpochSecond(asLong(payload.get("iat"))),
                    expiresAt
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid token payload");
        }
    }

    private String createToken(Map<String, Object> payload) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String signedContent = encodedHeader + "." + encodedPayload;
            return signedContent + "." + sign(signedContent);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create JWT", exception);
        }
    }

    private String sign(String content) {
        try {
            byte[] secret = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
            if (secret.length < 32) {
                throw new IllegalStateException("JWT secret must be at least 32 bytes");
            }
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return base64UrlEncode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    private byte[] base64UrlDecode(String input) {
        return Base64.getUrlDecoder().decode(input);
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
