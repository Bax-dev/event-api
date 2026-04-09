package com.digicore.eventapi.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private static final String   KEY_PREFIX = "idempotency:";
    private static final Duration TTL        = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public Optional<String> getCachedResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        try {
            String cached = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
            if (cached != null) log.debug("Idempotency cache hit for key: {}", idempotencyKey);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.warn("Idempotency get failed for key {}: {}", idempotencyKey, e.getMessage());
            return Optional.empty();
        }
    }

    public void storeResponse(String idempotencyKey, String responseJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, responseJson, TTL);
            log.debug("Idempotency key stored: {}", idempotencyKey);
        } catch (Exception e) {
            log.warn("Idempotency store failed for key {}: {}", idempotencyKey, e.getMessage());
        }
    }

    public void evict(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        try {
            redisTemplate.delete(KEY_PREFIX + idempotencyKey);
        } catch (Exception e) {
            log.warn("Idempotency evict failed for key {}: {}", idempotencyKey, e.getMessage());
        }
    }
}
