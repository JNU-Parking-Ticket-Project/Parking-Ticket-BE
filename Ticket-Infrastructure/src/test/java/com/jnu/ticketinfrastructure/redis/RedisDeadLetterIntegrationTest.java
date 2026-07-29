package com.jnu.ticketinfrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterQueueMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisDeadLetterIntegrationTest {

    private static final String STREAM_KEY = "stream:{3}";
    private static final String FAILURE_KEY = STREAM_KEY + ":failures";
    private static final String DEAD_LETTER_KEY = STREAM_KEY + ":dlq";
    private static final String GROUP = "group";

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, Object> redisTemplate;
    private static RedisRepository redisRepository;

    @BeforeAll
    static void setUp() {
        connectionFactory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        StringRedisSerializer serializer = new StringRedisSerializer();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(serializer);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        redisRepository = new RedisRepository(redisTemplate);
    }

    @AfterEach
    void flushRedis() {
        connectionFactory.getConnection().serverCommands().flushDb();
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("실패 상한에서 DLQ 이관, 원본 ACK·삭제, 중복 방지, 수동 재처리를 원자적으로 수행한다")
    void movesMessageOnceAndReplaysIt() throws Exception {
        ChatMessage message = new ChatMessage("{\"id\":10}", 1L, 2L, 3L);
        String payload = new ObjectMapper().writeValueAsString(message);
        String recordId = redisRepository.xAdd(STREAM_KEY, message).getValue();
        redisRepository.xReadGroupBlocking(
                STREAM_KEY, GROUP, "consumer", 10L, Duration.ofMillis(10));

        DeadLetterTransferResult first = recordFailure(recordId, payload, 3);
        DeadLetterTransferResult second = recordFailure(recordId, payload, 3);
        DeadLetterTransferResult third = recordFailure(recordId, payload, 3);
        DeadLetterTransferResult duplicate = recordFailure(recordId, payload, 3);

        assertThat(first.getFailureCount()).isEqualTo(1);
        assertThat(first.isMoved()).isFalse();
        assertThat(second.getFailureCount()).isEqualTo(2);
        assertThat(second.isMoved()).isFalse();
        assertThat(third.getFailureCount()).isEqualTo(3);
        assertThat(third.isMoved()).isTrue();
        assertThat(duplicate.isMoved()).isFalse();
        assertThat(redisRepository.xLength(STREAM_KEY)).isZero();
        assertThat(redisRepository.xPendingCount(STREAM_KEY, GROUP)).isZero();

        List<DeadLetterQueueMessage> deadLetters =
                redisRepository.xRangeDeadLetters(DEAD_LETTER_KEY, 10L);
        assertThat(deadLetters).hasSize(1);
        assertThat(deadLetters.get(0).getOriginalRecordId()).isEqualTo(recordId);
        assertThat(deadLetters.get(0).getPayload()).isEqualTo(payload);
        assertThat(deadLetters.get(0).getFailureCount()).isEqualTo(3);

        assertThat(
                        redisRepository.xReplayDeadLetter(
                                DEAD_LETTER_KEY, STREAM_KEY, FAILURE_KEY, deadLetters.get(0).getRecordId()))
                .isEqualTo(1L);
        assertThat(redisRepository.xLength(STREAM_KEY)).isEqualTo(1L);
        assertThat(redisRepository.xRangeDeadLetters(DEAD_LETTER_KEY, 10L)).isEmpty();
    }

    @Test
    @DisplayName("역직렬화할 수 없는 raw payload도 그대로 DLQ에 보존한다")
    void preservesMalformedPayload() {
        String payload = "not-json";
        String recordId =
                redisTemplate.opsForStream().add(STREAM_KEY, Map.of("payload", payload)).getValue();
        redisRepository.xReadGroupBlocking(
                STREAM_KEY, GROUP, "consumer", 10L, Duration.ofMillis(10));

        DeadLetterTransferResult result = recordFailure(recordId, payload, 1);

        assertThat(result.isMoved()).isTrue();
        assertThat(redisRepository.xRangeDeadLetters(DEAD_LETTER_KEY, 10L))
                .singleElement()
                .extracting(DeadLetterQueueMessage::getPayload)
                .isEqualTo(payload);
    }

    private DeadLetterTransferResult recordFailure(
            String recordId, String payload, int maxFailures) {
        return redisRepository.xRecordFailureAndMaybeMoveToDeadLetter(
                STREAM_KEY,
                FAILURE_KEY,
                DEAD_LETTER_KEY,
                GROUP,
                recordId,
                payload,
                maxFailures,
                "DB error",
                1_000L,
                "PROCESSING_FAILURE",
                1_000L,
                Duration.ofDays(7),
                false);
    }
}
