package com.jnu.ticketinfrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketinfrastructure.model.AutoClaimResult;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
import java.time.Duration;
import java.util.List;
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
class RedisRepositoryAutoClaimIntegrationTest {

    private static final String STREAM_KEY = "stream:{3}";
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
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
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
    @DisplayName("XAUTOCLAIM은 idle 기준을 넘긴 pending 메시지를 새 consumer로 회수한다")
    void autoClaimsStalePendingMessage() throws Exception {
        ChatMessage message = new ChatMessage("{\"id\":10}", 1L, 2L, 3L);
        String recordId = redisRepository.xAdd(STREAM_KEY, message).getValue();
        List<RawStreamMessage> delivered =
                redisRepository.xReadGroupBlocking(
                        STREAM_KEY, GROUP, "stopped-consumer", 10L, Duration.ofMillis(10));
        Thread.sleep(20L);

        AutoClaimResult claimed =
                redisRepository.xAutoClaim(
                        STREAM_KEY,
                        GROUP,
                        "replacement-consumer",
                        10L,
                        Duration.ofMillis(1),
                        "0-0");

        assertThat(delivered).extracting(RawStreamMessage::getRecordId).containsExactly(recordId);
        assertThat(claimed.messages()).hasSize(1);
        assertThat(claimed.messages().get(0).getRecordId()).isEqualTo(recordId);
        assertThat(claimed.messages().get(0).getPayload()).contains("{\\\"id\\\":10}");
        assertThat(redisRepository.xPendingCount(STREAM_KEY, GROUP)).isEqualTo(1L);
    }
}
