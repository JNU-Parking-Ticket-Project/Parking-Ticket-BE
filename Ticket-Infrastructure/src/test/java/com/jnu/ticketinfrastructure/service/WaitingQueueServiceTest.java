package com.jnu.ticketinfrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.AutoClaimResult;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterQueueMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
import com.jnu.ticketinfrastructure.model.SectorStockInitialization;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitingQueueServiceTest {

    private static final String STREAM_KEY = "event-issue-stream";

    @Mock private RedisRepository redisRepository;

    private WaitingQueueService waitingQueueService;

    @BeforeEach
    void setUp() {
        waitingQueueService = new WaitingQueueService(redisRepository);
    }

    @Test
    @DisplayName("신청 대기열 등록은 Redis Stream에 ChatMessage payload로 저장한다")
    void registerQueueAddsMessageToRedisStream() throws Exception {
        Registration registration = registration();

        waitingQueueService.registerQueue(STREAM_KEY, registration, 1L, 2L, 3L);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(redisRepository).xAdd(eq(STREAM_KEY), messageCaptor.capture());
        verify(redisRepository, never()).zAddIfAbsent(anyString(), any(), anyDouble());

        ChatMessage message = messageCaptor.getValue();
        assertThat(message.getUserId()).isEqualTo(1L);
        assertThat(message.getSectorId()).isEqualTo(2L);
        assertThat(message.getEventId()).isEqualTo(3L);

        JSONObject payload = new JSONObject(message.getRegistration());
        assertThat(payload.getLong("id")).isEqualTo(10L);
        assertThat(payload.getString("email")).isEqualTo("student@jnu.ac.kr");
        assertThat(payload.getString("studentNum")).isEqualTo("20240001");
        assertThat(payload.getBoolean("isSaved")).isFalse();
        assertThat(payload.getLong("eventId")).isEqualTo(3L);
    }

    @Test
    @DisplayName("Redis 예약은 DB 잔여여석과 이메일 중복 키, Stream 저장을 한 번에 위임한다")
    void reserveAndRegisterQueueDelegatesAtomicStockReservation() throws Exception {
        Registration registration = registration();
        Sector sector = org.mockito.Mockito.mock(Sector.class);
        when(sector.getId()).thenReturn(2L);
        when(sector.getInitSectorCapacity()).thenReturn(250);
        StockReservationResult reservationResult =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 299);
        when(redisRepository.reserveStockAndAddToStream(
                        eq("parking-ticket:event:{3}:sector:2:stock"),
                        eq("parking-ticket:event:{3}:sector:2:sequence"),
                        eq("parking-ticket:event:{3}:reserved:email"),
                        eq(STREAM_KEY),
                        eq("parking-ticket:event:{3}:closed"),
                        eq("parking-ticket:event:{3}:initialized"),
                        anyString(),
                        eq(1L),
                        eq(2L),
                        eq(3L),
                        eq("student@jnu.ac.kr"),
                        eq(250)))
                .thenReturn(reservationResult);

        StockReservationResult result =
                waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY, registration, 1L, sector, 3L);

        assertThat(result).isSameAs(reservationResult);
        ArgumentCaptor<String> registrationPayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisRepository)
                .reserveStockAndAddToStream(
                        eq("parking-ticket:event:{3}:sector:2:stock"),
                        eq("parking-ticket:event:{3}:sector:2:sequence"),
                        eq("parking-ticket:event:{3}:reserved:email"),
                        eq(STREAM_KEY),
                        eq("parking-ticket:event:{3}:closed"),
                        eq("parking-ticket:event:{3}:initialized"),
                        registrationPayloadCaptor.capture(),
                        eq(1L),
                        eq(2L),
                        eq(3L),
                        eq("student@jnu.ac.kr"),
                        eq(250));
        JSONObject payload = new JSONObject(registrationPayloadCaptor.getValue());
        assertThat(payload.getString("email")).isEqualTo("student@jnu.ac.kr");
        assertThat(payload.getString("studentNum")).isEqualTo("20240001");
    }

    @Test
    @DisplayName("이벤트 OPEN 초기화는 구간별 stock과 현재 position을 원자 설정한다")
    void initializeEventStockBuildsSectorStockState() {
        Sector sector = org.mockito.Mockito.mock(Sector.class);
        when(sector.getId()).thenReturn(2L);
        when(sector.getIssueAmount()).thenReturn(300);
        when(sector.getRemainingAmount()).thenReturn(240);
        when(redisRepository.initializeEventStock(
                        eq("parking-ticket:event:{3}:initialized"),
                        eq("parking-ticket:event:{3}:reserved:email"),
                        eq("parking-ticket:event:{3}:closed"),
                        any()))
                .thenReturn(true);

        boolean initialized = waitingQueueService.initializeEventStock(3L, List.of(sector));

        assertThat(initialized).isTrue();
        ArgumentCaptor<List<SectorStockInitialization>> initializationCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(redisRepository)
                .initializeEventStock(
                        eq("parking-ticket:event:{3}:initialized"),
                        eq("parking-ticket:event:{3}:reserved:email"),
                        eq("parking-ticket:event:{3}:closed"),
                        initializationCaptor.capture());
        SectorStockInitialization initialization = initializationCaptor.getValue().get(0);
        assertThat(initialization.getStockKey())
                .isEqualTo("parking-ticket:event:{3}:sector:2:stock");
        assertThat(initialization.getSequenceKey())
                .isEqualTo("parking-ticket:event:{3}:sector:2:sequence");
        assertThat(initialization.getRemainingAmount()).isEqualTo(240);
        assertThat(initialization.getAssignedPosition()).isEqualTo(60);
    }

    @Test
    @DisplayName("구간이 없는 이벤트는 Redis 초기화 마커를 만들지 않고 OPEN을 중단한다")
    void initializeEventStockRejectsEventWithoutSectors() {
        assertThatThrownBy(() -> waitingQueueService.initializeEventStock(3L, List.of()))
                .isSameAs(NotFoundSectorException.EXCEPTION);

        verifyNoInteractions(redisRepository);
    }

    @Test
    @DisplayName("새 메시지 조회는 blocking XREADGROUP에 위임한다")
    void readNewMessagesDelegatesToRedisStreamRepository() {
        List<RawStreamMessage> messages = List.of(new RawStreamMessage("1-0", "payload"));
        when(redisRepository.xReadGroupBlocking(
                        STREAM_KEY, "group", "consumer", 100L, Duration.ofMillis(500)))
                .thenReturn(messages);

        List<RawStreamMessage> result =
                waitingQueueService.readNewMessages(
                        STREAM_KEY, "group", "consumer", 100L, Duration.ofMillis(500));

        assertThat(result).containsExactlyElementsOf(messages);
    }

    @Test
    @DisplayName("stale pending 회수는 XAUTOCLAIM cursor와 idle 기준을 전달한다")
    void autoClaimMessagesDelegatesToRedisStreamRepository() {
        AutoClaimResult claimed =
                new AutoClaimResult("2-0", List.of(new RawStreamMessage("1-0", "payload")));
        when(redisRepository.xAutoClaim(
                        STREAM_KEY, "group", "consumer", 2L, Duration.ofSeconds(30), "0-0"))
                .thenReturn(claimed);

        AutoClaimResult result =
                waitingQueueService.autoClaimMessages(
                        STREAM_KEY, "group", "consumer", 2L, Duration.ofSeconds(30), "0-0");

        assertThat(result).isEqualTo(claimed);
    }

    @Test
    @DisplayName("처리 완료된 Stream record는 acknowledge로 ACK 처리한다")
    void acknowledgeDelegatesToRedisStreamRepository() {
        when(redisRepository.xAck(STREAM_KEY, "group", "1-0")).thenReturn(1L);

        Long acknowledged = waitingQueueService.acknowledge(STREAM_KEY, "group", "1-0");

        assertThat(acknowledged).isEqualTo(1L);
    }

    @Test
    @DisplayName("ACK와 원본 삭제, 실패 횟수 정리를 원자 연산에 위임한다")
    void acknowledgeAndDeleteDelegatesAtomicCleanup() {
        when(redisRepository.xAcknowledgeAndDelete(
                        STREAM_KEY, STREAM_KEY + ":failures", "group", "1-0"))
                .thenReturn(1L);

        Long acknowledged = waitingQueueService.acknowledgeAndDelete(STREAM_KEY, "group", "1-0");

        assertThat(acknowledged).isEqualTo(1L);
        verify(redisRepository)
                .xAcknowledgeAndDelete(STREAM_KEY, STREAM_KEY + ":failures", "group", "1-0");
    }

    @Test
    @DisplayName("처리 실패 횟수가 상한 미만이면 Pending을 유지한다")
    void recordProcessingFailureKeepsMessageUntilLimit() {
        String payload = "{\"registration\":\"{}\"}";
        DeadLetterTransferResult pending = new DeadLetterTransferResult(2, false);
        when(redisRepository.xRecordFailureAndMaybeMoveToDeadLetter(
                        eq(STREAM_KEY),
                        eq(STREAM_KEY + ":failures"),
                        eq(STREAM_KEY + ":dlq"),
                        eq("group"),
                        eq("1-0"),
                        eq(payload),
                        eq(3),
                        anyString(),
                        anyLong(),
                        eq("PROCESSING_FAILURE"),
                        eq(1_000L),
                        eq(Duration.ofDays(7)),
                        eq(false)))
                .thenReturn(pending);

        DeadLetterTransferResult result =
                waitingQueueService.recordProcessingFailure(
                        STREAM_KEY,
                        "group",
                        "1-0",
                        payload,
                        3,
                        new IllegalStateException("DB 저장 실패"));

        assertThat(result.getFailureCount()).isEqualTo(2);
        assertThat(result.isMoved()).isFalse();
    }

    @Test
    @DisplayName("이벤트 drain은 남은 모든 메시지를 DLQ로 강제 이관한다")
    void drainEventStreamMovesEveryRemainingMessage() {
        String eventStreamKey = "쿠폰 발급 스트림:{3}";
        RawStreamMessage first = new RawStreamMessage("1-0", "payload-1");
        RawStreamMessage second = new RawStreamMessage("2-0", "payload-2");
        when(redisRepository.xRangeRaw(eventStreamKey)).thenReturn(List.of(first, second));
        when(redisRepository.xRecordFailureAndMaybeMoveToDeadLetter(
                        eq(eventStreamKey),
                        eq(eventStreamKey + ":failures"),
                        eq(eventStreamKey + ":dlq"),
                        eq("group"),
                        anyString(),
                        anyString(),
                        eq(1),
                        anyString(),
                        anyLong(),
                        eq("EVENT_DRAIN_TIMEOUT"),
                        eq(1_000L),
                        eq(Duration.ofDays(7)),
                        eq(true)))
                .thenReturn(new DeadLetterTransferResult(0, true));

        long moved = waitingQueueService.drainEventStream(3L, "group");

        assertThat(moved).isEqualTo(2L);
        verify(redisRepository).expire(eventStreamKey, Duration.ofDays(1));
        verify(redisRepository).expire(eventStreamKey + ":failures", Duration.ofDays(1));
    }

    @Test
    @DisplayName("DLQ 조회 건수는 최대 보관 건수를 넘지 않는다")
    void findDeadLettersCapsRequestedCount() {
        DeadLetterQueueMessage message =
                new DeadLetterQueueMessage(
                        "9-0",
                        "1-0",
                        "payload",
                        3,
                        "error",
                        1_000L,
                        "PROCESSING_FAILURE");
        when(redisRepository.xRangeDeadLetters("쿠폰 발급 스트림:{3}:dlq", 1_000L))
                .thenReturn(List.of(message));

        List<DeadLetterQueueMessage> result = waitingQueueService.findDeadLetters(3L, 5_000L);

        assertThat(result).containsExactly(message);
    }

    @Test
    @DisplayName("DLQ 수동 재처리는 원본 Stream 복원을 원자 연산에 위임한다")
    void replayDeadLetterDelegatesAtomicReplay() {
        when(redisRepository.xReplayDeadLetter(
                        "쿠폰 발급 스트림:{3}:dlq", "쿠폰 발급 스트림:{3}", "쿠폰 발급 스트림:{3}:failures", "9-0"))
                .thenReturn(1L);

        boolean replayed = waitingQueueService.replayDeadLetter(3L, "9-0");

        assertThat(replayed).isTrue();
    }

    @Test
    @DisplayName("이벤트별 Stream key는 Redis Cluster hash tag에 eventId를 사용한다")
    void eventStreamKeyUsesEventHashTag() {
        assertThat(waitingQueueService.eventStreamKey(3L)).isEqualTo("쿠폰 발급 스트림:{3}");
        assertThat(waitingQueueService.eventDeadLetterStreamKey(3L)).isEqualTo("쿠폰 발급 스트림:{3}:dlq");
    }

    @Test
    @DisplayName("이벤트 삭제는 해당 이벤트의 Stream만 삭제한다")
    void deleteEventStreamDeletesOnlyEventStreamKey() {
        waitingQueueService.deleteEventStream(3L);

        verify(redisRepository).delete("쿠폰 발급 스트림:{3}");
    }

    @Test
    @DisplayName("Redis 잔여 재고 조회는 stock key를 사용한다")
    void findRemainingStockUsesStockKey() {
        when(redisRepository.getIntegerValue("parking-ticket:event:{3}:sector:2:stock"))
                .thenReturn(Optional.of(10));

        Optional<Integer> remainingStock = waitingQueueService.findRemainingStock(3L, 2L);

        assertThat(remainingStock).contains(10);
    }

    @Test
    @DisplayName("이벤트 종료 시 event 단위 Redis 재고 키 prefix를 삭제한다")
    void deleteEventStockKeysDeletesEventStockPrefix() {
        waitingQueueService.deleteEventStockKeys(3L);

        verify(redisRepository).deleteKeysByPrefix("parking-ticket:event:{3}:");
    }

    @Test
    @DisplayName("이벤트 종료 시 drain 기간 동안 Redis 재고 키 만료를 지연한다")
    void expireEventStockKeysUsesEventStockPrefix() {
        Duration timeout = Duration.ofMinutes(5);

        waitingQueueService.expireEventStockKeys(3L, timeout);

        verify(redisRepository).expireKeysByPrefix("parking-ticket:event:{3}:", timeout);
    }

    @Test
    @DisplayName("이벤트 종료 마커는 동일한 이벤트 hash slot에 TTL과 함께 저장한다")
    void markEventStockClosedUsesEventClosedKey() {
        Duration timeout = Duration.ofMinutes(5);

        waitingQueueService.markEventStockClosed(3L, timeout);

        verify(redisRepository).set("parking-ticket:event:{3}:closed", true, timeout);
    }

    private Registration registration() {
        Registration registration =
                Registration.builder()
                        .email("student@jnu.ac.kr")
                        .name("학생")
                        .studentNum("20240001")
                        .affiliation("공과대학")
                        .department("컴퓨터공학과")
                        .carNum("12가3456")
                        .isLight(false)
                        .phoneNum("010-0000-0000")
                        .createdAt(LocalDateTime.of(2026, 6, 25, 10, 0))
                        .isSaved(false)
                        .savedAt(null)
                        .eventId(3L)
                        .build();
        registration.setId(10L);
        return registration;
    }
}
