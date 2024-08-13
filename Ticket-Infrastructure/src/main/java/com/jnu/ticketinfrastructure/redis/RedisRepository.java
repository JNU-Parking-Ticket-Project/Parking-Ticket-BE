package com.jnu.ticketinfrastructure.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.Queue;
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

    public <T> Queue<T> zPopMin(String key, Long count, Class<T> type) {
        Set<T> set = (Set<T>) redisTemplate.opsForZSet().popMin(key, count);
        return new LinkedList<>(set);
    }

    public Object zPopMin(String key) {
        ZSetOperations.TypedTuple<Object> tuple = redisTemplate.opsForZSet().popMin(key);
        if (tuple != null) {
            return tuple.getValue();
        } else
            return null;
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

    public Long sRem(String key) {
        return redisTemplate.opsForSet().remove(key);
    }

    public Long sCard(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    public void converAndSend(String channel, ChatMessage chatMessage) {
        redisTemplate.convertAndSend(channel, chatMessage);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T> Set<Object> zReverseRange(String key, Long startRank, Long endRank, Class<T> type) {
        return redisTemplate.opsForZSet().reverseRange(key, startRank, endRank);
    }
}
