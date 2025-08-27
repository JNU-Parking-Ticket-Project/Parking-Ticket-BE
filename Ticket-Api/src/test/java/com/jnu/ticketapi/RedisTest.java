package com.jnu.ticketapi;

import com.jnu.ticketinfrastructure.redis.RedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class RedisTest {

    private static final int REDIS_PORT = 6379;

    @Container
    static GenericContainer redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
    }

    @Autowired
    private RedisRepository redisRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void clearRedis() {
        redisTemplate.execute((RedisCallback) connection -> {
            connection.flushDb();
            return null;
        });
    }


    @Test
    @DisplayName("레디스 스트림에서 첫번째 데이터를 조회힌다.")
    void readFirstRecord() {
        // given
        String key = "key";
        redisRepository.streamAdd(key, Map.of("eventId", "1"));
        redisRepository.streamAdd(key, Map.of("eventId", "2"));

        // when
        List<MapRecord<String, String, String>> mapRecords = redisRepository.streamReadAfterId(key, RecordId.of("0-0"), 1);


        // then
        MapRecord<String, String, String> mapRecord = mapRecords.get(0);
        Map<String, String> content = mapRecord.getValue();

        assertThat(content.get("eventId")).isEqualTo("1");
        assertThat(mapRecords).hasSize(1);
    }

    @Test
    @DisplayName("레디스 스트림에서 lastId로 데이터를 조회힌다.")
    void readById() {
        // given
        String key = "key";
        redisRepository.streamAdd(key, Map.of("eventId", "1"));
        redisRepository.streamAdd(key, Map.of("eventId", "2"));

        List<MapRecord<String, String, String>> mapRecords = redisRepository.streamReadAfterId(key, RecordId.of("0-0"), 1);
        RecordId lastId = mapRecords.get(0).getId();

        // when
        List<MapRecord<String, String, String>> results = redisRepository.streamReadAfterId(key, lastId, 1);

        // then
        MapRecord<String, String, String> result = results.get(0);
        Map<String, String> content = result.getValue();

        assertThat(content.get("eventId")).isEqualTo("2");
        assertThat(mapRecords).hasSize(1);
    }

}
