package com.jnu.ticketinfrastructure.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.stream.ByteRecord;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
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
    private static final String STREAM_REGISTRATION_KEY = "registration";
    private static final String STREAM_USER_ID_KEY = "userId";
    private static final String STREAM_SECTOR_ID_KEY = "sectorId";
    private static final String STREAM_EVENT_ID_KEY = "eventId";
    private static final String STREAM_POSITION_KEY = "position";
    private static final String STREAM_RESULT_STATUS_KEY = "resultStatus";
    private static final String STREAM_SEQUENCE_KEY = "sequence";
    private static final String RESERVE_STOCK_SCRIPT =
            "local stock = redis.call('GET', KEYS[1]) "
                    + "if not stock then "
                    + "  stock = tonumber(ARGV[1]) "
                    + "  redis.call('SET', KEYS[1], stock) "
                    + "else "
                    + "  stock = tonumber(stock) "
                    + "end "
                    + "if not redis.call('GET', KEYS[2]) then "
                    + "  redis.call('SET', KEYS[2], tonumber(ARGV[2]) - stock) "
                    + "end "
                    + "if redis.call('SISMEMBER', KEYS[3], ARGV[8]) == 1 then "
                    + "  return {0, 'DUPLICATE', -1, '', -1, stock} "
                    + "end "
                    + "if stock <= 0 then "
                    + "  return {0, 'NO_STOCK', -1, '', -1, stock} "
                    + "end "
                    + "local position = redis.call('INCR', KEYS[2]) "
                    + "local status = 'PREPARE' "
                    + "local sequence = position - tonumber(ARGV[3]) "
                    + "if position <= tonumber(ARGV[3]) then "
                    + "  status = 'SUCCESS' "
                    + "  sequence = -2 "
                    + "end "
                    + "local remaining = redis.call('DECR', KEYS[1]) "
                    + "redis.call('SADD', KEYS[3], ARGV[8]) "
                    + "redis.call('XADD', KEYS[4], '*', "
                    + "  'registration', ARGV[4], "
                    + "  'userId', ARGV[5], "
                    + "  'sectorId', ARGV[6], "
                    + "  'eventId', ARGV[7], "
                    + "  'position', tostring(position), "
                    + "  'resultStatus', status, "
                    + "  'sequence', tostring(sequence)) "
                    + "return {1, 'RESERVED', position, status, sequence, remaining}";
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

    public StockReservationResult reserveStockAndAddToStream(
            String stockKey,
            String sequenceKey,
            String reservedEmailKey,
            String streamKey,
            String registration,
            Long userId,
            Long sectorId,
            Long eventId,
            String email,
            Integer initialRemainingAmount,
            Integer issueAmount,
            Integer initSectorCapacity) {
        List<Object> rawResult =
                redisTemplate.execute(
                        (RedisCallback<List<Object>>)
                                connection ->
                                        (List<Object>)
                                                connection.eval(
                                                        RESERVE_STOCK_SCRIPT.getBytes(
                                                                StandardCharsets.UTF_8),
                                                        ReturnType.MULTI,
                                                        4,
                                                        redisArgs(
                                                                stockKey,
                                                                sequenceKey,
                                                                reservedEmailKey,
                                                                streamKey,
                                                                String.valueOf(
                                                                        initialRemainingAmount),
                                                                String.valueOf(issueAmount),
                                                                String.valueOf(initSectorCapacity),
                                                                registration,
                                                                String.valueOf(userId),
                                                                String.valueOf(sectorId),
                                                                String.valueOf(eventId),
                                                                email)));
        return toStockReservationResult(rawResult);
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

    public List<StreamQueueMessage> xClaimStale(
            String key, String group, String consumer, long count, Duration minIdleTime) {
        createConsumerGroupIfAbsent(key, group);
        PendingMessages pendingMessages =
                redisTemplate.opsForStream().pending(key, group, Range.unbounded(), count);
        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return List.of();
        }

        RecordId[] staleRecordIds =
                StreamSupport.stream(pendingMessages.spliterator(), false)
                        .filter(pendingMessage -> isStale(pendingMessage, minIdleTime))
                        .map(PendingMessage::getId)
                        .toArray(RecordId[]::new);
        if (staleRecordIds.length == 0) {
            return List.of();
        }

        List<ByteRecord> claimedRecords =
                redisTemplate.execute(
                        (RedisCallback<List<ByteRecord>>)
                                connection ->
                                        connection.xClaim(
                                                key.getBytes(StandardCharsets.UTF_8),
                                                group,
                                                consumer,
                                                minIdleTime,
                                                staleRecordIds));
        if (claimedRecords == null || claimedRecords.isEmpty()) {
            return List.of();
        }

        return claimedRecords.stream()
                .map(redisTemplate.opsForStream()::deserializeRecord)
                .map(this::toStreamQueueMessage)
                .toList();
    }

    public Long xAck(String key, String group, String recordId) {
        return redisTemplate.opsForStream().acknowledge(key, group, recordId);
    }

    public Long xDelete(String key, String recordId) {
        return redisTemplate.opsForStream().delete(key, recordId);
    }

    public Optional<Integer> getIntegerValue(String key) {
        byte[] value =
                redisTemplate.execute(
                        (RedisCallback<byte[]>)
                                connection -> connection.get(key.getBytes(StandardCharsets.UTF_8)));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(new String(value, StandardCharsets.UTF_8)));
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
            if (payload == null) {
                return new StreamQueueMessage(
                        record.getId().getValue(), toReservedChatMessage(record));
            }
            return new StreamQueueMessage(
                    record.getId().getValue(),
                    objectMapper.readValue(String.valueOf(payload), ChatMessage.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Redis Stream message", e);
        }
    }

    private ChatMessage toReservedChatMessage(MapRecord<String, Object, Object> record) {
        Map<Object, Object> values = record.getValue();
        return new ChatMessage(
                value(values, STREAM_REGISTRATION_KEY),
                longValue(values, STREAM_USER_ID_KEY),
                longValue(values, STREAM_SECTOR_ID_KEY),
                longValue(values, STREAM_EVENT_ID_KEY),
                integerValue(values, STREAM_POSITION_KEY),
                UserStatus.valueOf(value(values, STREAM_RESULT_STATUS_KEY)),
                integerValue(values, STREAM_SEQUENCE_KEY));
    }

    private StockReservationResult toStockReservationResult(List<Object> rawResult) {
        if (rawResult == null || rawResult.isEmpty()) {
            throw new IllegalStateException("Failed to reserve Redis stock");
        }
        boolean reserved = longValue(rawResult.get(0)) == 1L;
        String reason = stringValue(rawResult.get(1));
        Integer position = normalizePositive(integerValue(rawResult.get(2)));
        String status = stringValue(rawResult.get(3));
        Integer sequence = normalizeSequence(integerValue(rawResult.get(4)));
        Integer remainingAmount = integerValue(rawResult.get(5));

        if (reserved) {
            return StockReservationResult.reserved(
                    position, UserStatus.valueOf(status), sequence, remainingAmount);
        }
        if ("DUPLICATE".equals(reason)) {
            return StockReservationResult.duplicate(remainingAmount);
        }
        if ("NO_STOCK".equals(reason)) {
            return StockReservationResult.noStock(remainingAmount);
        }
        throw new IllegalStateException("Unknown Redis stock reservation result: " + reason);
    }

    private byte[][] redisArgs(String... values) {
        List<byte[]> args = new ArrayList<>();
        for (String value : values) {
            args.add(value.getBytes(StandardCharsets.UTF_8));
        }
        return args.toArray(new byte[0][]);
    }

    private String value(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            value = values.get(key.getBytes(StandardCharsets.UTF_8));
        }
        return stringValue(value);
    }

    private Long longValue(Map<Object, Object> values, String key) {
        return Long.parseLong(value(values, key));
    }

    private Integer integerValue(Map<Object, Object> values, String key) {
        return Integer.parseInt(value(values, key));
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(stringValue(value));
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(stringValue(value));
    }

    private String stringValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Redis value must not be null");
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    private Integer normalizePositive(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    private Integer normalizeSequence(Integer value) {
        return value != null && value != -1 ? value : null;
    }

    private boolean isAlreadyCreatedGroup(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("BUSYGROUP");
    }

    private boolean isStale(PendingMessage pendingMessage, Duration minIdleTime) {
        return pendingMessage.getElapsedTimeSinceLastDelivery().compareTo(minIdleTime) >= 0;
    }
}
