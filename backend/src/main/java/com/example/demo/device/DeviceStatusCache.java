package com.example.demo.device;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceStatusCache {
    private static final String KEY_PREFIX = "device:status:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final DeviceProperties properties;
    private final Map<Long, DeviceStatus> localStatuses = new ConcurrentHashMap<>();

    public DeviceStatusCache(ObjectProvider<StringRedisTemplate> redisTemplateProvider, DeviceProperties properties) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.properties = properties;
    }

    public void put(Long deviceId, DeviceStatus status) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(
                        KEY_PREFIX + deviceId,
                        status.name(),
                        Duration.ofSeconds(properties.getStatusCacheTtlSeconds())
                );
            }
        } catch (RuntimeException ignored) {
            // Redis cache is best-effort; the database remains the source of truth.
        }
        localStatuses.put(deviceId, status);
    }

    public Optional<DeviceStatus> get(Long deviceId) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                String value = redisTemplate.opsForValue().get(KEY_PREFIX + deviceId);
                if (value != null) {
                    return Optional.of(DeviceStatus.from(value));
                }
            }
        } catch (RuntimeException ignored) {
            // Fall through to local cache.
        }
        return Optional.ofNullable(localStatuses.get(deviceId));
    }

    public void remove(Long deviceId) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                redisTemplate.delete(KEY_PREFIX + deviceId);
            }
        } catch (RuntimeException ignored) {
            // Fall through to local cache removal.
        }
        localStatuses.remove(deviceId);
    }
}
