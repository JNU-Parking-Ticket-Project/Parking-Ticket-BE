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
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ByteRecord;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
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
    @Mock private ByteRecord byteRecord;

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
    @DisplayName("xReadGroup은 Stream record id와 payload를 StreamQueueMessage로 복원한다")
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

        List<StreamQueueMessage> result =
                redisRepository.xReadGroup(STREAM_KEY, GROUP, CONSUMER, 100);

        assertThat(result).hasSize(1);
        StreamQueueMessage streamQueueMessage = result.get(0);
        assertThat(streamQueueMessage.getRecordId()).isEqualTo("1690000000000-0");
        assertThat(streamQueueMessage.getMessage().getRegistration()).isEqualTo("{\"id\":10}");
        assertThat(streamQueueMessage.getMessage().getUserId()).isEqualTo(1L);
        assertThat(streamQueueMessage.getMessage().getSectorId()).isEqualTo(2L);
        assertThat(streamQueueMessage.getMessage().getEventId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("xReadGroup은 Redis 예약 script가 저장한 확정 결과 필드를 ChatMessage로 복원한다")
    void xReadGroupParsesReservedDecisionFields() {
        MapRecord<String, Object, Object> record =
                MapRecord.create(
                                STREAM_KEY,
                                Map.<Object, Object>of(
                                        "registration",
                                        "{\"id\":10}",
                                        "userId",
                                        "1",
                                        "sectorId",
                                        "2",
                                        "eventId",
                                        "3",
                                        "position",
                                        "4",
                                        "resultStatus",
                                        "PREPARE",
                                        "sequence",
                                        "2"))
                        .withId(RecordId.of("1690000000000-1"));
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("OK");
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.read(
                        any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(record));

        List<StreamQueueMessage> result =
                redisRepository.xReadGroup(STREAM_KEY, GROUP, CONSUMER, 100);

        ChatMessage message = result.get(0).getMessage();
        assertThat(message.getRegistration()).isEqualTo("{\"id\":10}");
        assertThat(message.getUserId()).isEqualTo(1L);
        assertThat(message.getSectorId()).isEqualTo(2L);
        assertThat(message.getEventId()).isEqualTo(3L);
        assertThat(message.getPosition()).isEqualTo(4);
        assertThat(message.getResultStatus()).isEqualTo(UserStatus.PREPARE);
        assertThat(message.getSequence()).isEqualTo(2);
        assertThat(message.hasDecision()).isTrue();
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
                        "{\"id\":10}",
                        1L,
                        2L,
                        3L,
                        "student@jnu.ac.kr",
                        300,
                        300,
                        250);

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
                        "{\"id\":10}",
                        1L,
                        2L,
                        3L,
                        "student@jnu.ac.kr",
                        300,
                        300,
                        250);

        assertThat(result.isReserved()).isFalse();
        assertThat(result.isNoStock()).isTrue();
        assertThat(result.getRemainingAmount()).isZero();
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
    @DisplayName("xClaimStale은 idle 기준을 넘긴 pending record를 현재 consumer로 가져온다")
    void xClaimStaleClaimsRecoverablePendingRecord() throws Exception {
        ChatMessage message = new ChatMessage("{\"id\":10}", 1L, 2L, 3L);
        MapRecord<String, Object, Object> record =
                MapRecord.create(
                                STREAM_KEY,
                                Map.<Object, Object>of(
                                        "payload", objectMapper.writeValueAsString(message)))
                        .withId(RecordId.of("1-0"));
        PendingMessage pendingMessage =
                new PendingMessage(
                        RecordId.of("1-0"),
                        Consumer.from(GROUP, "stopped-consumer"),
                        Duration.ofMinutes(1),
                        2L);
        PendingMessages pendingMessages = new PendingMessages(GROUP, List.of(pendingMessage));
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn("OK")
                .thenReturn(List.of(byteRecord));
        when(streamOperations.pending(
                        eq(STREAM_KEY),
                        eq(GROUP),
                        any(org.springframework.data.domain.Range.class),
                        eq(10L)))
                .thenReturn(pendingMessages);
        when(streamOperations.deserializeRecord(byteRecord)).thenReturn(record);

        List<StreamQueueMessage> result =
                redisRepository.xClaimStale(
                        STREAM_KEY, GROUP, CONSUMER, 10L, Duration.ofSeconds(30));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecordId()).isEqualTo("1-0");
        assertThat(result.get(0).getMessage().getUserId()).isEqualTo(1L);
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
}
