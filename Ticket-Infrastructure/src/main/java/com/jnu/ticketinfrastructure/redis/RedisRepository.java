package com.jnu.ticketinfrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class RedisRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public Boolean zAdd(String key, Object value, Double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    public Boolean zAddIfAbsent(String key, Object value, Double score) {
        return redisTemplate.opsForZSet().addIfAbsent(key, value, score);
    }

    public <T> Set<Object> zRange(String key, Long startRank, Long endRank, Class<T> type) {
        return redisTemplate.opsForZSet().range(key, startRank, endRank);
    }

    public <T> Set<T> zPopMin(String key, Long count, Class<T> type) {
        return (Set<T>) redisTemplate.opsForZSet().popMin(key, count);
    }

    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    public Long sAdd(String key, Object value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    public Long sRem(String key, Object value) {
        return redisTemplate.opsForSet().remove(key, value);
    }

    public Long sCard(String key) {
        return redisTemplate.opsForSet().size(key);
    }
}


