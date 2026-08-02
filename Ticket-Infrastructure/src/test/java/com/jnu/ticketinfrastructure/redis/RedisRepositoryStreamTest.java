package com.jnu.ticketinfrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterQueueMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
import com.jnu.ticketinfrastructure.model.SectorStockInitialization;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

@ExtendWith(MockitoExtension.class)
class RedisRepositoryStreamTest {

    private static final String STREAM_KEY = "event-issue-stream";
    private static final String GROUP = "event-issue-group";
    private static final String CONSUMER = "event-issue-consumer";

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private StreamOperations<String, Object, Object> streamOperations;
    private RedisRepository redisRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        redisRepository = new RedisRepository(redisTemplate);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("xAdd는 ChatMessage를 payload JSON으로 직렬화해서 Stream에 추가한다")
    void xAddSerializesChatMessageAsPayload() throws Exception {
        ChatMessage message = new ChatMessage("{\"id\":10}", 1L, 2L, 3L);
        RecordId recordId = RecordId.of("1-0");
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.add(eq(STREAM_KEY), any(Map.class))).thenReturn(recordId);

        RecordId result = redisRepository.xAdd(STREAM_KEY, message);

        assertThat(result).isEqualTo(recordId);
        ArgumentCaptor<Map<String, String>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq(STREAM_KEY), bodyCaptor.capture());

        String payload = bodyCaptor.getValue().get("payload");
        ChatMessage parsed = objectMapper.readValue(payload, ChatMessage.class);
        assertThat(parsed.getRegistration()).isEqualTo("{\"id\":10}");
        assertThat(parsed.getUserId()).isEqualTo(1L);
        assertThat(parsed.getSectorId()).isEqualTo(2L);
        assertThat(parsed.getEventId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("blocking XREADGROUP은 Stream record id와 raw payload를 복원한다")
    void xReadGroupParsesRecordIdAndPayload() throws Exception {
        ChatMessage message = new ChatMessage("{\"id\":10}", 1L, 2L, 3L);
        MapRecord<String, Object, Object> record =
                MapRecord.create(
                                STREAM_KEY,
                                Map.<Object, Object>of(
                                        "payload", objectMapper.writeValueAsString(message)))
                        .withId(RecordId.of("1690000000000-0"));
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("OK");
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.read(
                        any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(record));

        List<RawStreamMessage> result =
                redisRepository.xReadGroupBlocking(
                        STREAM_KEY, GROUP, CONSUMER, 100, Duration.ofMillis(500));

        assertThat(result).hasSize(1);
        RawStreamMessage streamMessage = result.get(0);
        assertThat(streamMessage.getRecordId()).isEqualTo("1690000000000-0");
        ChatMessage parsed = objectMapper.readValue(streamMessage.getPayload(), ChatMessage.class);
        assertThat(parsed.getRegistration()).isEqualTo("{\"id\":10}");
        assertThat(parsed.getUserId()).isEqualTo(1L);
        assertThat(parsed.getSectorId()).isEqualTo(2L);
        assertThat(parsed.getEventId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("blocking XREADGROUP은 Redis 예약 script의 확정 결과 필드를 raw payload로 복원한다")
    void xReadGroupParsesReservedDecisionFields() throws Exception {
        MapRecord<String, Object, Object> record =
                MapRecord.create(
                                STREAM_KEY,
                                Map.<Object, Object>ofEntries(
                                        Map.entry("registration", "{\"id\":10}"),
                                        Map.entry("userId", "1"),
                                        Map.entry("sectorId", "2"),
                                        Map.entry("eventId", "3"),
                                        Map.entry("position", "4"),
                                        Map.entry("resultStatus", "PREPARE"),
                                        Map.entry("sequence", "2"),
                                        Map.entry("remainingAmount", "296"),
                                        Map.entry("journalId", "100"),
                                        Map.entry("admissionEpoch", "7"),
                                        Map.entry("messageVersion", "2")))
                        .withId(RecordId.of("1690000000000-1"));
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("OK");
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.read(
                        any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(record));

        List<RawStreamMessage> result =
                redisRepository.xReadGroupBlocking(
                        STREAM_KEY, GROUP, CONSUMER, 100, Duration.ofMillis(500));

        ChatMessage message = objectMapper.readValue(result.get(0).getPayload(), ChatMessage.class);
        assertThat(message.getRegistration()).isEqualTo("{\"id\":10}");
        assertThat(message.getUserId()).isEqualTo(1L);
        assertThat(message.getSectorId()).isEqualTo(2L);
        assertThat(message.getEventId()).isEqualTo(3L);
        assertThat(message.getPosition()).isEqualTo(4);
        assertThat(message.getResultStatus()).isEqualTo(UserStatus.PREPARE);
        assertThat(message.getSequence()).isEqualTo(2);
        assertThat(message.getRemainingAmount()).isEqualTo(296);
        assertThat(message.getJournalId()).isEqualTo(100L);
        assertThat(message.getAdmissionEpoch()).isEqualTo(7L);
        assertThat(message.getMessageVersion()).isEqualTo(2);
        assertThat(message.hasDecision()).isTrue();
        assertThat(message.hasJournalDecision()).isTrue();
    }

    @Test
    @DisplayName("journal 메타데이터가 있어도 messageVersion 2가 아니면 journal 확정 메시지가 아니다")
    void journalDecisionRequiresMessageVersionTwo() {
        ChatMessage message =
                new ChatMessage(
                        "{\"id\":10}", 1L, 2L, 3L, 4, UserStatus.PREPARE, 2, 296, 100L, 7L, 1);

        assertThat(message.hasDecision()).isTrue();
        assertThat(message.hasJournalMetadata()).isTrue();
        assertThat(message.hasJournalDecision()).isFalse();
    }

    @Test
    @DisplayName("reserveStockAndAddToStream은 Redis Lua 결과를 예약 성공으로 변환한다")
    void reserveStockAndAddToStreamParsesReservedResult() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn(List.of(1L, "RESERVED", 1L, "SUCCESS", -2L, 299L));

        StockReservationResult result =
                redisRepository.reserveStockAndAddToStream(
                        "stock",
                        "sequence",
                        "reserved-email",
                        STREAM_KEY,
                        "closed",
                        "initialized",
                        "decision",
                        "{\"id\":10}",
                        1L,
                        2L,
                        3L,
                        "student@jnu.ac.kr",
                        250,
                        300,
                        100L,
                        7L);

        assertThat(result.isReserved()).isTrue();
        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(result.getResultStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(result.getSequence()).isEqualTo(-2);
        assertThat(result.getRemainingAmount()).isEqualTo(299);
    }

    @Test
    @DisplayName("reserveStockAndAddToStream은 Redis Lua 결과를 잔여 재고 없음으로 변환한다")
    void reserveStockAndAddToStreamParsesNoStockResult() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn(List.of(0L, "NO_STOCK", -1L, "", -1L, 0L));

        StockReservationResult result =
                redisRepository.reserveStockAndAddToStream(
                        "stock",
                        "sequence",
                        "reserved-email",
                        STREAM_KEY,
                        "closed",
                        "initialized",
                        "decision",
                        "{\"id\":10}",
                        1L,
                        2L,
                        3L,
                        "student@jnu.ac.kr",
                        250,
                        300,
                        100L,
                        7L);

        assertThat(result.isReserved()).isFalse();
        assertThat(result.isNoStock()).isTrue();
        assertThat(result.getRemainingAmount()).isZero();
    }

    @Test
    @DisplayName("reserveStockAndAddToStream은 종료 마커 결과를 이벤트 종료로 변환한다")
    void reserveStockAndAddToStreamParsesClosedResult() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn(List.of(0L, "CLOSED", -1L, "", -1L, 17L));

        StockReservationResult result =
                redisRepository.reserveStockAndAddToStream(
                        "stock",
                        "sequence",
                        "reserved-email",
                        STREAM_KEY,
                        "closed",
                        "initialized",
                        "decision",
                        "{\"id\":10}",
                        1L,
                        2L,
                        3L,
                        "student@jnu.ac.kr",
                        250,
                        300,
                        100L,
                        7L);

        assertThat(result.isReserved()).isFalse();
        assertThat(result.isClosed()).isTrue();
        assertThat(result.getRemainingAmount()).isEqualTo(17);
    }

    @Test
    @DisplayName("reserveStockAndAddToStream은 필수 Redis 상태 유실을 unavailable로 변환한다")
    void reserveStockAndAddToStreamParsesUnavailableResult() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn(List.of(0L, "UNAVAILABLE", -1L, "", -1L, -1L));

        StockReservationResult result =
                redisRepository.reserveStockAndAddToStream(
                        "stock",
                        "sequence",
                        "reserved-email",
                        STREAM_KEY,
                        "closed",
                        "initialized",
                        "decision",
                        "{\"id\":10}",
                        1L,
                        2L,
                        3L,
                        "student@jnu.ac.kr",
                        250,
                        300,
                        100L,
                        7L);

        assertThat(result.isUnavailable()).isTrue();
        assertThat(result.getRemainingAmount()).isNull();
    }

    @Test
    @DisplayName("initializeEventStock은 원자 초기화 Lua 결과를 반환한다")
    void initializeEventStockReturnsAtomicScriptResult() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);
        SectorStockInitialization sector =
                new SectorStockInitialization("stock", "sequence", 240, 60);

        boolean initialized =
                redisRepository.initializeEventStock(
                        "initialized", "reserved-email", "closed", List.of(sector));

        assertThat(initialized).isTrue();
    }

    @Test
    @DisplayName("getIntegerValue는 Lua가 저장한 raw 숫자 문자열을 serializer 없이 조회한다")
    void getIntegerValueReadsRawLuaValue() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn("239".getBytes(StandardCharsets.UTF_8));

        assertThat(redisRepository.getIntegerValue("stock")).contains(239);
    }

    @Test
    @DisplayName("expireKeysByPrefix는 조회된 모든 키에 동일한 TTL을 설정한다")
    void expireKeysByPrefixExpiresEveryMatchingKey() {
        Duration timeout = Duration.ofMinutes(5);
        when(redisTemplate.keys("parking-ticket:event:{3}:*"))
                .thenReturn(Set.of("stock", "sequence"));

        redisRepository.expireKeysByPrefix("parking-ticket:event:{3}:", timeout);

        verify(redisTemplate).expire("stock", timeout);
        verify(redisTemplate).expire("sequence", timeout);
    }

    @Test
    @DisplayName("consumer group이 이미 있으면 BUSYGROUP 예외를 성공으로 처리한다")
    void createConsumerGroupIgnoresBusyGroup() {
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(
                        new RedisSystemException(
                                "BUSYGROUP Consumer Group name already exists",
                                new RuntimeException()));

        assertThatCode(() -> redisRepository.createConsumerGroupIfAbsent(STREAM_KEY, GROUP))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("xAck는 Stream record ACK를 Redis에 위임한다")
    void xAckDelegatesToStreamOperations() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.acknowledge(STREAM_KEY, GROUP, "1-0")).thenReturn(1L);

        Long acknowledged = redisRepository.xAck(STREAM_KEY, GROUP, "1-0");

        assertThat(acknowledged).isEqualTo(1L);
    }

    @Test
    @DisplayName("xDelete는 처리 완료된 Stream record 삭제를 Redis에 위임한다")
    void xDeleteDelegatesToStreamOperations() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.delete(STREAM_KEY, "1-0")).thenReturn(1L);

        Long deleted = redisRepository.xDelete(STREAM_KEY, "1-0");

        assertThat(deleted).isEqualTo(1L);
    }

    @Test
    @DisplayName("xAcknowledgeAndDelete는 Lua의 ACK 결과를 반환한다")
    void xAcknowledgeAndDeleteReturnsAtomicScriptResult() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);

        Long acknowledged =
                redisRepository.xAcknowledgeAndDelete(
                        STREAM_KEY, STREAM_KEY + ":failures", GROUP, "1-0");

        assertThat(acknowledged).isEqualTo(1L);
    }

    @Test
    @DisplayName("실패 기록 Lua 결과를 실패 횟수와 DLQ 이관 여부로 변환한다")
    void xRecordFailureAndMaybeMoveToDeadLetterParsesResult() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(List.of(3L, 1L));

        DeadLetterTransferResult result =
                redisRepository.xRecordFailureAndMaybeMoveToDeadLetter(
                        STREAM_KEY,
                        STREAM_KEY + ":failures",
                        STREAM_KEY + ":dlq",
                        GROUP,
                        "1-0",
                        "{\"registration\":\"{}\"}",
                        3,
                        "DB error",
                        1_000L,
                        "PROCESSING_FAILURE",
                        1_000L,
                        Duration.ofDays(7),
                        false);

        assertThat(result.getFailureCount()).isEqualTo(3);
        assertThat(result.isMoved()).isTrue();
    }

    @Test
    @DisplayName("DLQ Stream record는 원본 payload와 실패 메타데이터로 복원한다")
    void xRangeDeadLettersParsesFailureMetadata() throws Exception {
        ChatMessage message = new ChatMessage("{\"id\":10}", 1L, 2L, 3L);
        MapRecord<String, Object, Object> record =
                MapRecord.create(
                                STREAM_KEY + ":dlq",
                                Map.<Object, Object>of(
                                        "payload",
                                        objectMapper.writeValueAsString(message),
                                        "originalRecordId",
                                        "1-0",
                                        "failureCount",
                                        "3",
                                        "lastError",
                                        "DB error",
                                        "failedAt",
                                        "1000",
                                        "reason",
                                        "PROCESSING_FAILURE"))
                        .withId(RecordId.of("9-0"));
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.range(
                        STREAM_KEY + ":dlq", org.springframework.data.domain.Range.unbounded()))
                .thenReturn(List.of(record));

        List<DeadLetterQueueMessage> result =
                redisRepository.xRangeDeadLetters(STREAM_KEY + ":dlq", 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecordId()).isEqualTo("9-0");
        assertThat(result.get(0).getOriginalRecordId()).isEqualTo("1-0");
        assertThat(result.get(0).getFailureCount()).isEqualTo(3);
        assertThat(result.get(0).getLastError()).isEqualTo("DB error");
        assertThat(
                        objectMapper
                                .readValue(result.get(0).getPayload(), ChatMessage.class)
                                .getUserId())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("DLQ 재처리는 Lua의 원본 Stream 복원 결과를 반환한다")
    void xReplayDeadLetterReturnsAtomicScriptResult() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);

        Long replayed =
                redisRepository.xReplayDeadLetter(
                        STREAM_KEY + ":dlq", STREAM_KEY, STREAM_KEY + ":failures", "9-0");

        assertThat(replayed).isEqualTo(1L);
    }
}
