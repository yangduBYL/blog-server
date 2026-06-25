package com.example.blog.service;

import com.example.blog.config.CacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCacheService {

    private static final String DETAIL_PREFIX = "article:cache:detail:";
    private static final String LIST_PREFIX = "article:cache:list:";
    private static final String ADMIN_DETAIL_PREFIX = "article:cache:admin:";
    private static final String COUNT_KEY = "article:cache:count:total";
    private static final String LOCK_SUFFIX = ":lock";
    private static final String NULL_PLACEHOLDER = "__NULL__";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheProperties cacheProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public Duration randomTtl() {
        int jitter = cacheProperties.getJitterMinutes() > 0
                ? ThreadLocalRandom.current().nextInt(cacheProperties.getJitterMinutes())
                : 0;
        return Duration.ofMinutes(cacheProperties.getBaseTtlMinutes() + jitter);
    }

    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (NULL_PLACEHOLDER.equals(cached)) {
            return null;
        }
        if (cached != null) {
            return convert(cached, type);
        }

        String lockKey = key + LOCK_SUFFIX;
        boolean locked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(cacheProperties.getLockExpireSeconds())));

        if (locked) {
            try {
                cached = redisTemplate.opsForValue().get(key);
                if (NULL_PLACEHOLDER.equals(cached)) {
                    return null;
                }
                if (cached != null) {
                    return convert(cached, type);
                }
                T loaded = loader.get();
                if (loaded == null) {
                    redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER,
                            Duration.ofMinutes(cacheProperties.getNullTtlMinutes()));
                } else {
                    redisTemplate.opsForValue().set(key, loaded, randomTtl());
                }
                return loaded;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        try {
            Thread.sleep(cacheProperties.getLockWaitMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cached = redisTemplate.opsForValue().get(key);
        if (cached != null && !NULL_PLACEHOLDER.equals(cached)) {
            return convert(cached, type);
        }
        return loader.get();
    }

    public void put(String key, Object value) {
        if (value == null) {
            redisTemplate.opsForValue().set(key, NULL_PLACEHOLDER,
                    Duration.ofMinutes(cacheProperties.getNullTtlMinutes()));
        } else {
            redisTemplate.opsForValue().set(key, value, randomTtl());
        }
    }

    public void evictDetail(String slug) {
        if (slug != null) {
            redisTemplate.delete(DETAIL_PREFIX + slug);
        }
    }

    public void evictAdminDetail(Long id) {
        if (id != null) {
            redisTemplate.delete(ADMIN_DETAIL_PREFIX + id);
        }
    }

    public void evictListCaches() {
        var keys = redisTemplate.keys(LIST_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void evictCountCache() {
        redisTemplate.delete(COUNT_KEY);
    }

    public void evictAllArticleCaches(String slug, Long id) {
        evictDetail(slug);
        evictAdminDetail(id);
        evictListCaches();
        evictCountCache();
    }

    public String detailKey(String slug) {
        return DETAIL_PREFIX + slug;
    }

    public String listKey(int page, int size, String tag) {
        return LIST_PREFIX + page + ":" + size + ":" + (tag == null ? "all" : tag);
    }

    public String adminDetailKey(Long id) {
        return ADMIN_DETAIL_PREFIX + id;
    }

    public String countKey() {
        return COUNT_KEY;
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(Object cached, Class<T> type) {
        if (type.isInstance(cached)) {
            return (T) cached;
        }
        return objectMapper.convertValue(cached, type);
    }
}
