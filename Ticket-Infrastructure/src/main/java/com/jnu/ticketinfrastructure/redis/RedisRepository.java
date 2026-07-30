package com.jnu.ticketinfrastructure.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.AutoClaimResult;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterQueueMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
import com.jnu.ticketinfrastructure.model.SectorStockInitialization;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import io.lettuce.core.XAutoClaimArgs;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.models.stream.ClaimedMessages;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
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
    private static final String INITIALIZE_EVENT_STOCK_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end "
                    + "redis.call('DEL', KEYS[2]) "
                    + "redis.call('DEL', KEYS[3]) "
                    + "local argumentIndex = 1 "
                    + "for keyIndex = 4, #KEYS, 2 do "
                    + "  redis.call('SET', KEYS[keyIndex], ARGV[argumentIndex]) "
                    + "  redis.call('SET', KEYS[keyIndex + 1], ARGV[argumentIndex + 1]) "
                    + "  argumentIndex = argumentIndex + 2 "
                    + "end "
                    + "redis.call('SET', KEYS[1], '1') "
                    + "return 1";
    private static final String REBUILD_EVENT_STOCK_SCRIPT =
            "redis.call('DEL', KEYS[1]) "
                    + "redis.call('DEL', KEYS[2]) "
                    + "redis.call('DEL', KEYS[3]) "
                    + "local argumentIndex = 1 "
                    + "for keyIndex = 4, #KEYS, 2 do "
                    + "  redis.call('SET', KEYS[keyIndex], ARGV[argumentIndex]) "
                    + "  redis.call('SET', KEYS[keyIndex + 1], ARGV[argumentIndex + 1]) "
                    + "  argumentIndex = argumentIndex + 2 "
                    + "end "
                    + "for emailIndex = argumentIndex, #ARGV do "
                    + "  redis.call('SADD', KEYS[2], ARGV[emailIndex]) "
                    + "end "
                    + "redis.call('SET', KEYS[1], '1') "
                    + "return 1";
    private static final String DEAD_LETTER_ORIGINAL_RECORD_ID_KEY = "originalRecordId";
    private static final String DEAD_LETTER_FAILURE_COUNT_KEY = "failureCount";
    private static final String DEAD_LETTER_LAST_ERROR_KEY = "lastError";
    private static final String DEAD_LETTER_FAILED_AT_KEY = "failedAt";
    private static final String DEAD_LETTER_REASON_KEY = "reason";
    private static final String ACKNOWLEDGE_AND_DELETE_SCRIPT =
            "local acknowledged = redis.call('XACK', KEYS[1], ARGV[1], ARGV[2]) "
                    + "if acknowledged > 0 then "
                    + "  redis.call('XDEL', KEYS[1], ARGV[2]) "
                    + "  redis.call('HDEL', KEYS[2], ARGV[2]) "
                    + "end "
                    + "return acknowledged";
    private static final String MOVE_TO_DEAD_LETTER_SCRIPT =
            "local records = redis.call('XRANGE', KEYS[1], ARGV[2], ARGV[2]) "
                    + "if #records == 0 then return {0, 0} end "
                    + "local failureCount = tonumber(redis.call('HGET', KEYS[2], ARGV[2]) or '0') "
                    + "if ARGV[10] == '0' then "
                    + "  failureCount = redis.call('HINCRBY', KEYS[2], ARGV[2], 1) "
                    + "  redis.call('EXPIRE', KEYS[2], tonumber(ARGV[9])) "
                    + "  if failureCount < tonumber(ARGV[3]) then return {failureCount, 0} end "
                    + "end "
                    + "redis.call('XADD', KEYS[3], 'MAXLEN', tonumber(ARGV[8]), '*', "
                    + "  'payload', ARGV[4], "
                    + "  'originalRecordId', ARGV[2], "
                    + "  'failureCount', tostring(failureCount), "
                    + "  'lastError', ARGV[5], "
                    + "  'failedAt', ARGV[6], "
                    + "  'reason', ARGV[7]) "
                    + "redis.call('EXPIRE', KEYS[3], tonumber(ARGV[9])) "
                    + "redis.pcall('XACK', KEYS[1], ARGV[1], ARGV[2]) "
                    + "redis.call('XDEL', KEYS[1], ARGV[2]) "
                    + "redis.call('HDEL', KEYS[2], ARGV[2]) "
                    + "return {failureCount, 1}";
    private static final String REPLAY_DEAD_LETTER_SCRIPT =
            "local records = redis.call('XRANGE', KEYS[1], ARGV[1], ARGV[1]) "
                    + "if #records == 0 then return 0 end "
                    + "local fields = records[1][2] "
                    + "local payload = nil "
                    + "local originalRecordId = nil "
                    + "for index = 1, #fields, 2 do "
                    + "  if fields[index] == 'payload' then payload = fields[index + 1] end "
                    + "  if fields[index] == 'originalRecordId' then originalRecordId = fields[index + 1] end "
                    + "end "
                    + "if not payload then return -1 end "
                    + "redis.call('XADD', KEYS[2], '*', 'payload', payload) "
                    + "redis.call('XDEL', KEYS[1], ARGV[1]) "
                    + "if originalRecordId then redis.call('HDEL', KEYS[3], originalRecordId) end "
                    + "return 1";
    private static final String RESERVE_STOCK_SCRIPT =
            "if redis.call('EXISTS', KEYS[5]) == 1 then "
                    + "  local closedStock = redis.call('GET', KEYS[1]) "
                    + "  if not closedStock then closedStock = 0 end "
                    + "  return {0, 'CLOSED', -1, '', -1, tonumber(closedStock)} "
                    + "end "
                    + "if redis.call('EXISTS', KEYS[6]) == 0 then "
                    + "  return {0, 'UNAVAILABLE', -1, '', -1, -1} "
                    + "end "
                    + "local stock = redis.call('GET', KEYS[1]) "
                    + "local positionValue = redis.call('GET', KEYS[2]) "
                    + "if not stock or not positionValue then "
                    + "  return {0, 'UNAVAILABLE', -1, '', -1, -1} "
                    + "end "
                    + "stock = tonumber(stock) "
                    + "if redis.call('SISMEMBER', KEYS[3], ARGV[6]) == 1 then "
                    + "  return {0, 'DUPLICATE', -1, '', -1, stock} "
                    + "end "
                    + "if stock <= 0 then "
                    + "  return {0, 'NO_STOCK', -1, '', -1, stock} "
                    + "end "
                    + "local position = redis.call('INCR', KEYS[2]) "
                    + "local status = 'PREPARE' "
                    + "local sequence = position - tonumber(ARGV[1]) "
                    + "if position <= tonumber(ARGV[1]) then "
                    + "  status = 'SUCCESS' "
                    + "  sequence = -2 "
                    + "end "
                    + "local remaining = redis.call('DECR', KEYS[1]) "
                    + "redis.call('SADD', KEYS[3], ARGV[6]) "
                    + "redis.call('XADD', KEYS[4], '*', "
                    + "  'registration', ARGV[2], "
                    + "  'userId', ARGV[3], "
                    + "  'sectorId', ARGV[4], "
                    + "  'eventId', ARGV[5], "
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

    public void expireKeysByPrefix(String prefix, Duration timeout) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null) {
            keys.forEach(key -> redisTemplate.expire(key, timeout));
        }
    }

    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
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
            String closedKey,
            String initializedKey,
            String registration,
            Long userId,
            Long sectorId,
            Long eventId,
            String email,
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
                                                        6,
                                                        redisArgs(
                                                                stockKey,
                                                                sequenceKey,
                                                                reservedEmailKey,
                                                                streamKey,
                                                                closedKey,
                                                                initializedKey,
                                                                String.valueOf(initSectorCapacity),
                                                                registration,
                                                                String.valueOf(userId),
                                                                String.valueOf(sectorId),
                                                                String.valueOf(eventId),
                                                                email)));
        return toStockReservationResult(rawResult);
    }

    public boolean initializeEventStock(
            String initializedKey,
            String reservedEmailKey,
            String closedKey,
            List<SectorStockInitialization> sectors) {
        List<String> keys = new ArrayList<>();
        keys.add(initializedKey);
        keys.add(reservedEmailKey);
        keys.add(closedKey);
        List<String> arguments = new ArrayList<>();
        for (SectorStockInitialization sector : sectors) {
            keys.add(sector.getStockKey());
            keys.add(sector.getSequenceKey());
            arguments.add(String.valueOf(sector.getRemainingAmount()));
            arguments.add(String.valueOf(sector.getAssignedPosition()));
        }
        List<String> scriptParameters = new ArrayList<>(keys);
        scriptParameters.addAll(arguments);
        Long initialized =
                redisTemplate.execute(
                        (RedisCallback<Long>)
                                connection ->
                                        connection.eval(
                                                INITIALIZE_EVENT_STOCK_SCRIPT.getBytes(
                                                        StandardCharsets.UTF_8),
                                                ReturnType.INTEGER,
                                                keys.size(),
                                                redisArgs(
                                                        scriptParameters.toArray(new String[0]))));
        return initialized != null && initialized == 1L;
    }

    public boolean rebuildEventStock(
            String initializedKey,
            String reservedEmailKey,
            String closedKey,
            List<SectorStockInitialization> sectors,
            Set<String> reservedEmails) {
        List<String> keys = new ArrayList<>();
        keys.add(initializedKey);
        keys.add(reservedEmailKey);
        keys.add(closedKey);
        List<String> arguments = new ArrayList<>();
        for (SectorStockInitialization sector : sectors) {
            keys.add(sector.getStockKey());
            keys.add(sector.getSequenceKey());
            arguments.add(String.valueOf(sector.getRemainingAmount()));
            arguments.add(String.valueOf(sector.getAssignedPosition()));
        }
        arguments.addAll(reservedEmails);
        List<String> scriptParameters = new ArrayList<>(keys);
        scriptParameters.addAll(arguments);
        Long rebuilt =
                redisTemplate.execute(
                        (RedisCallback<Long>)
                                connection ->
                                        connection.eval(
                                                REBUILD_EVENT_STOCK_SCRIPT.getBytes(
                                                        StandardCharsets.UTF_8),
                                                ReturnType.INTEGER,
                                                keys.size(),
                                                redisArgs(
                                                        scriptParameters.toArray(new String[0]))));
        return rebuilt != null && rebuilt == 1L;
    }

    public boolean ping() {
        String response =
                redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
        return "PONG".equalsIgnoreCase(response);
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

    public List<RawStreamMessage> xReadGroupBlocking(
            String key, String group, String consumer, long count, Duration blockTimeout) {
        createConsumerGroupIfAbsent(key, group);
        List<MapRecord<String, Object, Object>> records =
                redisTemplate
                        .opsForStream()
                        .read(
                                Consumer.from(group, consumer),
                                StreamReadOptions.empty().count(count).block(blockTimeout),
                                StreamOffset.create(key, ReadOffset.lastConsumed()));
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream().map(this::toRawStreamMessage).toList();
    }

    @SuppressWarnings("unchecked")
    public AutoClaimResult xAutoClaim(
            String key,
            String group,
            String consumer,
            long count,
            Duration minIdleTime,
            String startId) {
        createConsumerGroupIfAbsent(key, group);
        return redisTemplate.execute(
                (RedisCallback<AutoClaimResult>)
                        connection -> {
                            RedisClusterAsyncCommands<byte[], byte[]> commands =
                                    (RedisClusterAsyncCommands<byte[], byte[]>)
                                            connection.getNativeConnection();
                            XAutoClaimArgs<byte[]> args =
                                    new XAutoClaimArgs<byte[]>()
                                            .consumer(
                                                    io.lettuce.core.Consumer.from(
                                                            bytes(group), bytes(consumer)))
                                            .minIdleTime(minIdleTime)
                                            .startId(startId)
                                            .count(count);
                            try {
                                ClaimedMessages<byte[], byte[]> claimedMessages =
                                        commands.xautoclaim(bytes(key), args)
                                                .get(1, TimeUnit.SECONDS);
                                List<RawStreamMessage> messages =
                                        claimedMessages.getMessages().stream()
                                                .map(
                                                        message ->
                                                                new RawStreamMessage(
                                                                        message.getId(),
                                                                        payload(message.getBody())))
                                                .toList();
                                return new AutoClaimResult(claimedMessages.getId(), messages);
                            } catch (Exception e) {
                                throw new IllegalStateException(
                                        "Failed to auto-claim Redis Stream messages", e);
                            }
                        });
    }

    public long xLength(String key) {
        Long size = redisTemplate.opsForStream().size(key);
        return size == null ? 0L : size;
    }

    public long xPendingCount(String key, String group) {
        PendingMessagesSummary summary = redisTemplate.opsForStream().pending(key, group);
        return summary == null ? 0L : summary.getTotalPendingMessages();
    }

    public Long xAck(String key, String group, String recordId) {
        return redisTemplate.opsForStream().acknowledge(key, group, recordId);
    }

    public Long xDelete(String key, String recordId) {
        return redisTemplate.opsForStream().delete(key, recordId);
    }

    public Long xAcknowledgeAndDelete(
            String streamKey, String failureKey, String group, String recordId) {
        return redisTemplate.execute(
                (RedisCallback<Long>)
                        connection ->
                                connection.eval(
                                        ACKNOWLEDGE_AND_DELETE_SCRIPT.getBytes(
                                                StandardCharsets.UTF_8),
                                        ReturnType.INTEGER,
                                        2,
                                        redisArgs(streamKey, failureKey, group, recordId)));
    }

    public DeadLetterTransferResult xRecordFailureAndMaybeMoveToDeadLetter(
            String streamKey,
            String failureKey,
            String deadLetterKey,
            String group,
            String recordId,
            String payload,
            int maxFailures,
            String lastError,
            long failedAt,
            String reason,
            long maxLength,
            Duration retention,
            boolean forceMove) {
        List<Object> rawResult =
                redisTemplate.execute(
                        (RedisCallback<List<Object>>)
                                connection ->
                                        (List<Object>)
                                                connection.eval(
                                                        MOVE_TO_DEAD_LETTER_SCRIPT.getBytes(
                                                                StandardCharsets.UTF_8),
                                                        ReturnType.MULTI,
                                                        3,
                                                        redisArgs(
                                                                streamKey,
                                                                failureKey,
                                                                deadLetterKey,
                                                                group,
                                                                recordId,
                                                                String.valueOf(maxFailures),
                                                                payload,
                                                                lastError,
                                                                String.valueOf(failedAt),
                                                                reason,
                                                                String.valueOf(maxLength),
                                                                String.valueOf(
                                                                        retention.toSeconds()),
                                                                forceMove ? "1" : "0")));
        if (rawResult == null || rawResult.size() < 2) {
            throw new IllegalStateException("Failed to move Redis Stream message to DLQ");
        }
        return new DeadLetterTransferResult(
                integerValue(rawResult.get(0)), longValue(rawResult.get(1)) == 1L);
    }

    public List<RawStreamMessage> xRangeRaw(String key) {
        List<MapRecord<String, Object, Object>> records =
                redisTemplate.opsForStream().range(key, Range.unbounded());
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream().map(this::toRawStreamMessage).toList();
    }

    public List<DeadLetterQueueMessage> xRangeDeadLetters(String key, long count) {
        List<MapRecord<String, Object, Object>> records =
                redisTemplate.opsForStream().range(key, Range.unbounded());
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream().limit(count).map(this::toDeadLetterQueueMessage).toList();
    }

    public Long xReplayDeadLetter(
            String deadLetterKey, String streamKey, String failureKey, String recordId) {
        return redisTemplate.execute(
                (RedisCallback<Long>)
                        connection ->
                                connection.eval(
                                        REPLAY_DEAD_LETTER_SCRIPT.getBytes(StandardCharsets.UTF_8),
                                        ReturnType.INTEGER,
                                        3,
                                        redisArgs(deadLetterKey, streamKey, failureKey, recordId)));
    }

    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(key, timeout);
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

    private RawStreamMessage toRawStreamMessage(MapRecord<String, Object, Object> record) {
        Object payload = record.getValue().get(STREAM_PAYLOAD_KEY);
        try {
            if (payload == null) {
                payload = objectMapper.writeValueAsString(toReservedChatMessage(record));
            }
            return new RawStreamMessage(record.getId().getValue(), String.valueOf(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Redis Stream message", e);
        }
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String payload(Map<byte[], byte[]> body) {
        Optional<String> payload =
                body.entrySet().stream()
                        .filter(
                                entry ->
                                        STREAM_PAYLOAD_KEY.equals(
                                                new String(entry.getKey(), StandardCharsets.UTF_8)))
                        .map(entry -> new String(entry.getValue(), StandardCharsets.UTF_8))
                        .findFirst();
        if (payload.isPresent()) {
            return payload.get();
        }
        Map<Object, Object> values =
                body.entrySet().stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        entry -> new String(entry.getKey(), StandardCharsets.UTF_8),
                                        entry ->
                                                new String(
                                                        entry.getValue(), StandardCharsets.UTF_8)));
        try {
            return objectMapper.writeValueAsString(toReservedChatMessage(values));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Redis Stream message", e);
        }
    }

    private DeadLetterQueueMessage toDeadLetterQueueMessage(
            MapRecord<String, Object, Object> record) {
        Map<Object, Object> values = record.getValue();
        return new DeadLetterQueueMessage(
                record.getId().getValue(),
                value(values, DEAD_LETTER_ORIGINAL_RECORD_ID_KEY),
                value(values, STREAM_PAYLOAD_KEY),
                integerValue(values, DEAD_LETTER_FAILURE_COUNT_KEY),
                value(values, DEAD_LETTER_LAST_ERROR_KEY),
                longValue(values, DEAD_LETTER_FAILED_AT_KEY),
                value(values, DEAD_LETTER_REASON_KEY));
    }

    private ChatMessage toReservedChatMessage(MapRecord<String, Object, Object> record) {
        return toReservedChatMessage(record.getValue());
    }

    private ChatMessage toReservedChatMessage(Map<Object, Object> values) {
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
        if ("CLOSED".equals(reason)) {
            return StockReservationResult.closed(remainingAmount);
        }
        if ("UNAVAILABLE".equals(reason)) {
            return StockReservationResult.unavailable(
                    remainingAmount != null && remainingAmount >= 0 ? remainingAmount : null);
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
}
