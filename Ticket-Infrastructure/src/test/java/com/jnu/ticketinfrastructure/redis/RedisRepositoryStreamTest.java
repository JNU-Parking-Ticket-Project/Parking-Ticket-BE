package com.jnu.ticketinfrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
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
}
