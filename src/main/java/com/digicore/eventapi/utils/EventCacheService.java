package com.digicore.eventapi.utils;

import com.digicore.eventapi.dto.response.EventResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCacheService {

    private static final String   PREFIX = "event:";
    private static final Duration TTL    = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    public Optional<EventResponse> get(String eventId) {
        try {
            String json = redisTemplate.opsForValue().get(PREFIX + eventId);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, EventResponse.class));
        } catch (Exception e) {
            log.warn("Cache get failed for event {}: {}", eventId, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(EventResponse response) {
        try {
            redisTemplate.opsForValue().set(
                    PREFIX + response.getId(),
                    objectMapper.writeValueAsString(response),
                    TTL);
        } catch (Exception e) {
            log.warn("Cache put failed for event {}: {}", response.getId(), e.getMessage());
        }
    }

    public void evict(String eventId) {
        try {
            redisTemplate.delete(PREFIX + eventId);
        } catch (Exception e) {
            log.warn("Cache evict failed for event {}: {}", eventId, e.getMessage());
        }
    }
}
