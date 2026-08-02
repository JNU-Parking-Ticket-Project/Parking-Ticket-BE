package com.jnu.ticketapi.redis;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.SectorStockInitialization;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisStockReservationConcurrencyTest {

    private static final int REDIS_PORT = 6379;

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(REDIS_PORT);

    private LettuceConnectionFactory connectionFactory;
    private RedisRepository redisRepository;
    private final AtomicLong journalSequence = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        connectionFactory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
        connectionFactory.afterPropertiesSet();

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        StringRedisSerializer serializer = new StringRedisSerializer();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(serializer);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        redisRepository = new RedisRepository(redisTemplate);

        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    @Timeout(value = 45)
    @DisplayName("3천 건 동시 예약에서 발급량만 승인하고 position과 결과를 중복 없이 확정한다")
    void reservesOnlyIssueAmountUnderThreeThousandConcurrentRequests() throws Exception {
        int requestCount = 3_000;
        int capacity = 250;
        int issueAmount = 300;
        ExecutorService executor = Executors.newFixedThreadPool(64);
        CountDownLatch startSignal = new CountDownLatch(1);
        initialize(1L, 1L, issueAmount, issueAmount);

        try {
            List<CompletableFuture<StockReservationResult>> futures =
                    IntStream.range(0, requestCount)
                            .mapToObj(
                                    index ->
                                            CompletableFuture.supplyAsync(
                                                    () -> {
                                                        await(startSignal);
                                                        return reserve(
                                                                1L,
                                                                1L,
                                                                "student" + index + "@jnu.ac.kr",
                                                                capacity,
                                                                issueAmount);
                                                    },
                                                    executor))
                            .toList();
            startSignal.countDown();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(40, SECONDS);
            List<StockReservationResult> results =
                    futures.stream().map(CompletableFuture::join).toList();
            List<StockReservationResult> reserved =
                    results.stream().filter(StockReservationResult::isReserved).toList();

            assertThat(reserved).hasSize(issueAmount);
            assertThat(results)
                    .filteredOn(StockReservationResult::isNoStock)
                    .hasSize(requestCount - issueAmount);
            assertThat(reserved)
                    .extracting(StockReservationResult::getPosition)
                    .containsExactlyInAnyOrderElementsOf(
                            IntStream.rangeClosed(1, issueAmount).boxed().toList());
            assertThat(reserved)
                    .filteredOn(result -> result.getResultStatus() == UserStatus.SUCCESS)
                    .hasSize(capacity);
            assertThat(reserved)
                    .filteredOn(result -> result.getResultStatus() == UserStatus.PREPARE)
                    .hasSize(issueAmount - capacity)
                    .extracting(StockReservationResult::getSequence)
                    .containsExactlyInAnyOrderElementsOf(
                            IntStream.rangeClosed(1, issueAmount - capacity).boxed().toList());
            assertThat(redisRepository.getIntegerValue(stockKey(1L, 1L))).contains(0);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Redis 초기화 후 DB OPEN이 실패해도 같은 재고로 재시도할 수 있다")
    void retriesIdenticalInitializationAfterDatabaseOpenFailure() {
        assertThat(initialize(20L, 1L, 300, 300)).isTrue();

        assertThat(initialize(20L, 1L, 300, 300)).isTrue();
        assertThat(redisRepository.getIntegerValue(stockKey(20L, 1L))).contains(300);
        assertThat(redisRepository.getIntegerValue(sequenceKey(20L, 1L))).contains(0);
    }

    @Test
    @DisplayName("이미 신청이 시작된 Redis 재고는 OPEN 초기화 재시도로 덮지 않는다")
    void rejectsInitializationWhenAdmissionAlreadyStarted() {
        assertThat(initialize(21L, 1L, 300, 300)).isTrue();
        assertThat(reserve(21L, 1L, "student@jnu.ac.kr", 250, 300).isReserved()).isTrue();

        assertThat(initialize(21L, 1L, 300, 300)).isFalse();
        assertThat(redisRepository.getIntegerValue(stockKey(21L, 1L))).contains(299);
        assertThat(redisRepository.getIntegerValue(sequenceKey(21L, 1L))).contains(1);
    }

    @Test
    @DisplayName("OPEN 초기화 시 DB 잔여여석 다음 position부터 예약하고 이메일 중복은 차감하지 않는다")
    void reservesFromExplicitlyInitializedPositionAndRejectsDuplicateEmail() {
        initialize(2L, 1L, 240, 300);
        StockReservationResult first = reserve(2L, 1L, "student@jnu.ac.kr", 250);
        StockReservationResult duplicate = reserve(2L, 1L, "student@jnu.ac.kr", 250);

        assertThat(first.isReserved()).isTrue();
        assertThat(first.getPosition()).isEqualTo(61);
        assertThat(first.getResultStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(first.getRemainingAmount()).isEqualTo(239);
        assertThat(duplicate.isDuplicate()).isTrue();
        assertThat(duplicate.getRemainingAmount()).isEqualTo(239);
        assertThat(redisRepository.getIntegerValue(stockKey(2L, 1L))).contains(239);
    }

    @Test
    @DisplayName("같은 journal ID 재시도는 최초 예약 결과를 반환하고 재고와 Stream을 중복 변경하지 않는다")
    void retryWithSameJournalIdReturnsOriginalDecision() {
        initialize(8L, 1L, 300, 300);

        StockReservationResult first = reserve(8L, 1L, "student@jnu.ac.kr", 250, 300, 100L, 7L);
        StockReservationResult next = reserve(8L, 1L, "next@jnu.ac.kr", 250);
        StockReservationResult retried = reserve(8L, 1L, "student@jnu.ac.kr", 250, 300, 100L, 7L);

        assertThat(next.getPosition()).isEqualTo(2);
        assertThat(retried.isReserved()).isTrue();
        assertThat(retried.getPosition()).isEqualTo(first.getPosition());
        assertThat(retried.getResultStatus()).isEqualTo(first.getResultStatus());
        assertThat(retried.getSequence()).isEqualTo(first.getSequence());
        assertThat(retried.getRemainingAmount()).isEqualTo(first.getRemainingAmount());
        assertThat(redisRepository.getIntegerValue(stockKey(8L, 1L))).contains(298);
        assertThat(redisRepository.getIntegerValue(sequenceKey(8L, 1L))).contains(2);
        assertThat(redisRepository.xLength(streamKey(8L))).isEqualTo(2L);
    }

    @Test
    @DisplayName("stock과 sequence 합계 불변식이 깨지면 예약과 Stream 변경 없이 unavailable을 반환한다")
    void rejectsReservationWhenStockAndSequenceInvariantIsBroken() {
        initialize(9L, 1L, 300, 300);
        redisRepository.set(stockKey(9L, 1L), "298", Duration.ofMinutes(1));

        StockReservationResult unavailable = reserve(9L, 1L, "student@jnu.ac.kr", 250);

        assertThat(unavailable.isUnavailable()).isTrue();
        assertThat(redisRepository.getIntegerValue(stockKey(9L, 1L))).contains(298);
        assertThat(redisRepository.getIntegerValue(sequenceKey(9L, 1L))).contains(0);
        assertThat(redisRepository.xLength(streamKey(9L))).isZero();
    }

    @Test
    @DisplayName("이벤트 종료 마커 이후에는 재고와 Stream을 변경하지 않는다")
    void rejectsReservationAfterEventIsClosed() {
        initialize(3L, 1L, 300, 300);
        StockReservationResult first = reserve(3L, 1L, "first@jnu.ac.kr", 250);
        redisRepository.set(closedKey(3L), "true", java.time.Duration.ofMinutes(5));

        StockReservationResult closed = reserve(3L, 1L, "late@jnu.ac.kr", 250);

        assertThat(first.isReserved()).isTrue();
        assertThat(closed.isClosed()).isTrue();
        assertThat(closed.getRemainingAmount()).isEqualTo(299);
        assertThat(redisRepository.getIntegerValue(stockKey(3L, 1L))).contains(299);
        assertThat(redisRepository.xLength(streamKey(3L))).isEqualTo(1L);
    }

    @Test
    @DisplayName("initialized 마커가 유실되면 DB 값으로 재생성하지 않고 예약을 차단한다")
    void rejectsReservationWhenInitializedMarkerIsLost() {
        initialize(4L, 1L, 300, 300);
        StockReservationResult first = reserve(4L, 1L, "first@jnu.ac.kr", 250);
        redisRepository.delete(initializedKey(4L));

        StockReservationResult unavailable = reserve(4L, 1L, "second@jnu.ac.kr", 250);

        assertThat(first.isReserved()).isTrue();
        assertThat(unavailable.isUnavailable()).isTrue();
        assertThat(redisRepository.getIntegerValue(stockKey(4L, 1L))).contains(299);
        assertThat(redisRepository.getIntegerValue(sequenceKey(4L, 1L))).contains(1);
        assertThat(
                        redisRepository.xReadGroupBlocking(
                                streamKey(4L),
                                "lost-marker",
                                "consumer",
                                10,
                                Duration.ofMillis(10)))
                .hasSize(1);
    }

    @Test
    @DisplayName("initialized 마커가 있어도 구간 stock이 유실되면 position과 Stream을 변경하지 않는다")
    void rejectsReservationWhenSectorStockIsLost() {
        initialize(5L, 1L, 300, 300);
        redisRepository.delete(stockKey(5L, 1L));

        StockReservationResult unavailable = reserve(5L, 1L, "student@jnu.ac.kr", 250);

        assertThat(unavailable.isUnavailable()).isTrue();
        assertThat(redisRepository.getIntegerValue(stockKey(5L, 1L))).isEmpty();
        assertThat(redisRepository.getIntegerValue(sequenceKey(5L, 1L))).contains(0);
        assertThat(
                        redisRepository.xReadGroupBlocking(
                                streamKey(5L), "lost-stock", "consumer", 10, Duration.ofMillis(10)))
                .isEmpty();
    }

    @Test
    @DisplayName("중복 OPEN 초기화는 이미 사용한 재고와 position을 덮어쓰지 않는다")
    void duplicateInitializationDoesNotResetStockState() {
        assertThat(initialize(6L, 1L, 300, 300)).isTrue();
        assertThat(reserve(6L, 1L, "student@jnu.ac.kr", 250).isReserved()).isTrue();

        boolean reinitialized = initialize(6L, 1L, 300, 300);

        assertThat(reinitialized).isFalse();
        assertThat(redisRepository.getIntegerValue(stockKey(6L, 1L))).contains(299);
        assertThat(redisRepository.getIntegerValue(sequenceKey(6L, 1L))).contains(1);
    }

    @Test
    @DisplayName("DB 스냅샷 복구 후 기존 신청은 중복 차단하고 다음 position부터 예약한다")
    void rebuildsAdmissionStateFromDatabaseSnapshot() {
        initialize(7L, 1L, 300, 300);
        assertThat(reserve(7L, 1L, "redis@jnu.ac.kr", 250).isReserved()).isTrue();
        redisRepository.delete(initializedKey(7L));
        redisRepository.delete(stockKey(7L, 1L));
        redisRepository.delete(sequenceKey(7L, 1L));
        redisRepository.delete("parking-ticket:event:{7}:reserved:email");

        boolean rebuilt =
                redisRepository.rebuildEventStock(
                        initializedKey(7L),
                        "parking-ticket:event:{7}:reserved:email",
                        closedKey(7L),
                        List.of(
                                new SectorStockInitialization(
                                        stockKey(7L, 1L), sequenceKey(7L, 1L), 297, 3)),
                        Set.of("redis@jnu.ac.kr", "fallback@jnu.ac.kr"));
        StockReservationResult duplicate = reserve(7L, 1L, "fallback@jnu.ac.kr", 250);
        StockReservationResult next = reserve(7L, 1L, "next@jnu.ac.kr", 250);

        assertThat(rebuilt).isTrue();
        assertThat(duplicate.isDuplicate()).isTrue();
        assertThat(duplicate.getRemainingAmount()).isEqualTo(297);
        assertThat(next.isReserved()).isTrue();
        assertThat(next.getPosition()).isEqualTo(4);
        assertThat(next.getRemainingAmount()).isEqualTo(296);
        assertThat(redisRepository.getIntegerValue(sequenceKey(7L, 1L))).contains(4);
        assertThat(redisRepository.xLength(streamKey(7L))).isEqualTo(2L);
    }

    private StockReservationResult reserve(
            Long eventId, Long sectorId, String email, int capacity) {
        return reserve(
                eventId, sectorId, email, capacity, 300, journalSequence.getAndIncrement(), 1L);
    }

    private StockReservationResult reserve(
            Long eventId, Long sectorId, String email, int capacity, int issueAmount) {
        return reserve(
                eventId,
                sectorId,
                email,
                capacity,
                issueAmount,
                journalSequence.getAndIncrement(),
                1L);
    }

    private StockReservationResult reserve(
            Long eventId,
            Long sectorId,
            String email,
            int capacity,
            int issueAmount,
            Long journalId,
            Long admissionEpoch) {
        return redisRepository.reserveStockAndAddToStream(
                stockKey(eventId, sectorId),
                sequenceKey(eventId, sectorId),
                "parking-ticket:event:{" + eventId + "}:reserved:email",
                streamKey(eventId),
                closedKey(eventId),
                initializedKey(eventId),
                decisionKey(eventId),
                "{\"email\":\"" + email + "\"}",
                (long) email.hashCode(),
                sectorId,
                eventId,
                email,
                capacity,
                issueAmount,
                journalId,
                admissionEpoch);
    }

    private boolean initialize(Long eventId, Long sectorId, int remainingAmount, int issueAmount) {
        return redisRepository.initializeEventStock(
                initializedKey(eventId),
                "parking-ticket:event:{" + eventId + "}:reserved:email",
                closedKey(eventId),
                List.of(
                        new SectorStockInitialization(
                                stockKey(eventId, sectorId),
                                sequenceKey(eventId, sectorId),
                                remainingAmount,
                                issueAmount - remainingAmount)));
    }

    private String closedKey(Long eventId) {
        return "parking-ticket:event:{" + eventId + "}:closed";
    }

    private String streamKey(Long eventId) {
        return "쿠폰 발급 스트림:{" + eventId + "}";
    }

    private String initializedKey(Long eventId) {
        return "parking-ticket:event:{" + eventId + "}:initialized";
    }

    private String decisionKey(Long eventId) {
        return "parking-ticket:event:{" + eventId + "}:decision:journal";
    }

    private String stockKey(Long eventId, Long sectorId) {
        return "parking-ticket:event:{" + eventId + "}:sector:" + sectorId + ":stock";
    }

    private String sequenceKey(Long eventId, Long sectorId) {
        return "parking-ticket:event:{" + eventId + "}:sector:" + sectorId + ":sequence";
    }

    private void await(CountDownLatch startSignal) {
        try {
            startSignal.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to start", e);
        }
    }
}
