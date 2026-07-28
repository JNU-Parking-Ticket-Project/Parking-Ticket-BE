package com.jnu.ticketinfrastructure.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@ConditionalOnExpression("${ableRedis:true}")
public class RedisRepository {
    private static final String STREAM_PAYLOAD_KEY = "payload";
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

    public Set<ZSetOperations.TypedTuple<Object>> zRangeWithScores(
            String key, Long startRank, Long endRank) {
        return redisTemplate.opsForZSet().rangeWithScores(key, startRank, endRank);
    }

    public <T> Queue<T> zPopMin(String key, Long count, Class<T> type) {
        Set<T> set = (Set<T>) redisTemplate.opsForZSet().popMin(key, count);
        return new LinkedList<>(set);
    }

    public Object zPopMin(String key) {
        String luaScript =
                "local result = redis.call('ZRANGE', KEYS[1], 0, 0) "
                        + "if result ~= nil and #result > 0 then "
                        + "    redis.call('ZREM', KEYS[1], result[1]) "
                        + "    return result[1] "
                        + "else "
                        + "    return nil "
                        + "end";

        return redisTemplate.execute(
                (RedisCallback<Object>)
                        connection ->
                                connection.eval(
                                        luaScript.getBytes(), ReturnType.VALUE, 1, key.getBytes()));
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

    public void deleteKeysByPrefix(String prefix) {
        // 1. 해당 prefix로 시작하는 모든 키 검색
        Set<String> keys = redisTemplate.keys(prefix + "*");

        // 2. 검색된 키들이 존재하는지 확인하고, 존재하면 삭제
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public Double getScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public RecordId xAdd(String key, ChatMessage message) {
        try {
            Map<String, String> body =
                    Map.of(STREAM_PAYLOAD_KEY, objectMapper.writeValueAsString(message));
            return redisTemplate.opsForStream().add(key, body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to add message to Redis Stream", e);
        }
    }

    public void createConsumerGroupIfAbsent(String key, String group) {
        try {
            redisTemplate.execute(
                    (RedisCallback<String>)
                            connection ->
                                    connection.xGroupCreate(
                                            key.getBytes(StandardCharsets.UTF_8),
                                            group,
                                            ReadOffset.from("0-0"),
                                            true));
        } catch (DataAccessException e) {
            if (!isAlreadyCreatedGroup(e)) {
                throw e;
            }
        }
    }

    public List<StreamQueueMessage> xReadGroup(
            String key, String group, String consumer, long count) {
        createConsumerGroupIfAbsent(key, group);
        List<MapRecord<String, Object, Object>> records =
                redisTemplate
                        .opsForStream()
                        .read(
                                Consumer.from(group, consumer),
                                StreamReadOptions.empty().count(count),
                                StreamOffset.create(key, ReadOffset.lastConsumed()));
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream().map(this::toStreamQueueMessage).toList();
    }

    public Long xAck(String key, String group, String recordId) {
        return redisTemplate.opsForStream().acknowledge(key, group, recordId);
    }

    public Long xDelete(String key, String recordId) {
        return redisTemplate.opsForStream().delete(key, recordId);
    }

    public Long remove(String key, Object value) {
        return redisTemplate.opsForZSet().remove(key, value);
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

    private StreamQueueMessage toStreamQueueMessage(MapRecord<String, Object, Object> record) {
        Object payload = record.getValue().get(STREAM_PAYLOAD_KEY);
        try {
            return new StreamQueueMessage(
                    record.getId().getValue(),
                    objectMapper.readValue(String.valueOf(payload), ChatMessage.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Redis Stream message", e);
        }
    }

    private boolean isAlreadyCreatedGroup(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("BUSYGROUP");
    }
}
