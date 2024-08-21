package com.jnu.ticketinfrastructure.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.ChatMessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Repository
@Slf4j
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
        String luaScript =
                "local result = redis.call('ZRANGE', KEYS[1], 0, 0) " +
                        "if result ~= nil and #result > 0 then " +
                        "    redis.call('ZREM', KEYS[1], result[1]) " +
                        "    return result[1] " +
                        "else " +
                        "    return nil " +
                        "end";

        // Log before executing the script
        log.info("Executing Lua script to pop min element from key: {}", key);

        Object poppedValue = redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.eval(luaScript.getBytes(), ReturnType.VALUE, 1, key.getBytes())
        );

        // Log after executing the script
        if (poppedValue != null) {
            log.info("Popped value from Redis: {}", poppedValue);
        } else {
            log.info("No value was popped, the set might be empty or another issue occurred.");
        }

        return poppedValue;
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

    public void deleteKeysByPrefix(String prefix) {
        // 1. 해당 prefix로 시작하는 모든 키 검색
        Set<String> keys = redisTemplate.keys(prefix + "*");

        // 2. 검색된 키들이 존재하는지 확인하고, 존재하면 삭제
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void removeAndAdd(String key, ChatMessage message, ChatMessageStatus newStatus, Double score) {
        log.info("Re-registering the message in Redis");

        // Update the status of the message
        message.setStatus(newStatus.name());

        // Lua script for atomic removal and addition
        String luaScript =
                "local removed = redis.call('ZREM', KEYS[1], ARGV[1]) " +
                        "if removed > 0 then " +
                        "   return redis.call('ZADD', KEYS[1], ARGV[2], ARGV[3]) " +
                        "else " +
                        "   return 0 " +
                        "end";

        // Execute Lua script
        Long result = redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.eval(luaScript.getBytes(), ReturnType.INTEGER, 1,
                        key.getBytes(),
                        message.toString().getBytes(),
                        score.toString().getBytes(),
                        message.toString().getBytes()
                )
        );

        log.info("Re-registration result: {}", result > 0 ? "Success" : "Failure");
    }

    public Double getScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public void remove(String key, Object value) {
        redisTemplate.opsForZSet().remove(key, value);
    }

    public Object getValue(String key) {
        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();

        // Get the element with the smallest score
        Set<ZSetOperations.TypedTuple<Object>> tuples = zSetOperations.rangeWithScores(key, 0, 0);

        if (!tuples.isEmpty()) {
            // Get the first tuple (element with the smallest score)
            ZSetOperations.TypedTuple<Object> tuple = tuples.iterator().next();

            // Return the removed element
            return tuple.getValue();
        } else {
            // Set is empty, return null or handle accordingly
            return null;
        }
    }
}
