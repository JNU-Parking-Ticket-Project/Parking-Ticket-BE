package com.jnu.ticketapi;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
public class FlowTest {

    public static final int REDIS_PORT = 6379;
    public static final int MYSQL_PORT = 3306;

    @Container
    static MySQLContainer mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.40"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
        registry.add("spring.datasource.url", () -> String.format(
                "jdbc:mysql://%s:%d/%s", mysqlContainer.getHost(), mysqlContainer.getMappedPort(MYSQL_PORT), mysqlContainer.getDatabaseName())
        );
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }

    WebTestClient client;

    @BeforeEach
    void setup(WebApplicationContext context) {
        client = MockMvcWebTestClient.bindToApplicationContext(context).build();
    }

}


