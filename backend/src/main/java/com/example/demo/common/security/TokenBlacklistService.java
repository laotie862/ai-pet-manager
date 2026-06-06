package com.example.demo.common.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private static final String KEY_PREFIX = "auth:blacklist:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, Instant> localBlacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public void blacklist(String token, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(KEY_PREFIX + token, "1", ttl);
                return;
            }
        } catch (RuntimeException ignored) {
            // Redis is an infrastructure optimization here; local fallback keeps logout deterministic.
        }

        localBlacklist.put(token, expiresAt);
    }

    public boolean isBlacklisted(String token) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token))) {
                return true;
            }
        } catch (RuntimeException ignored) {
            // Fall through to local fallback.
        }

        Instant expiresAt = localBlacklist.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            localBlacklist.remove(token);
            return false;
        }
        return true;
    }
}
