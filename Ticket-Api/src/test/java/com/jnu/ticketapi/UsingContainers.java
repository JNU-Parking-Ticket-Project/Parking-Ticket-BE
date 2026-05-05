package com.jnu.ticketapi;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public interface UsingContainers {

    int REDIS_PORT = 6379;
    int MYSQL_PORT = 3306;

    @Container
    MySQLContainer mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.40"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    GenericContainer redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
        registry.add("spring.datasource.url", () -> String.format(
                "jdbc:mysql://%s:%d/%s", mysqlContainer.getHost(), mysqlContainer.getMappedPort(MYSQL_PORT), mysqlContainer.getDatabaseName())
        );
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }
}
