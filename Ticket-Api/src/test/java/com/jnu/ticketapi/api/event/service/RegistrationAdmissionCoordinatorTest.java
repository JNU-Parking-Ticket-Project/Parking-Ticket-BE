package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.RedisStockUnavailableException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionJournal;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationDecisionSource;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationAdmissionCoordinatorTest {

    private static final long EVENT_ID = 10L;
    private static final long SECTOR_ID = 20L;
    private static final long USER_ID = 30L;
    private static final long JOURNAL_ID = 40L;
    private static final long ADMISSION_EPOCH = 7L;
    private static final String EMAIL = "student@jnu.ac.kr";
    private static final String STREAM_KEY = "쿠폰 발급 스트림:{10}";
    private static final String REGISTRATION_PAYLOAD = "{\"email\":\"student@jnu.ac.kr\"}";

    @Mock private RegistrationResultPersistenceService registrationResultPersistenceService;
    @Mock private RegistrationAdmissionJournalService registrationAdmissionJournalService;
    @Mock private EventAdmissionControlService eventAdmissionControlService;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private Registration registration;
    @Mock private Sector sector;

    private RegistrationAdmissionCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator =
                new RegistrationAdmissionCoordinator(
                        registrationResultPersistenceService,
                        registrationAdmissionJournalService,
                        eventAdmissionControlService);
        ReflectionTestUtils.setField(coordinator, "waitingQueueService", waitingQueueService);
        org.mockito.Mockito.lenient().when(sector.getId()).thenReturn(SECTOR_ID);
        org.mockito.Mockito.lenient().when(registration.getEmail()).thenReturn(EMAIL);
        org.mockito.Mockito.lenient()
                .when(waitingQueueService.eventStreamKey(EVENT_ID))
                .thenReturn(STREAM_KEY);
        org.mockito.Mockito.lenient()
                .when(waitingQueueService.convertRegistrationJSON(registration))
                .thenReturn(REGISTRATION_PAYLOAD);
    }

    @Test
    @DisplayName("Redis 예약 결과만 확정하고 본 신청 DB 저장은 Stream 처리에 맡긴다")
    void admitsAsynchronouslyWithRedis() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult reserved =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 2);
        when(registrationAdmissionJournalService.openJournal(
                        eq(registration),
                        eq(USER_ID),
                        eq(SECTOR_ID),
                        eq(EVENT_ID),
                        eq(REGISTRATION_PAYLOAD),
                        anyLong()))
                .thenReturn(attempt(journal, EventAdmissionMode.REDIS, false));
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenReturn(reserved);
        when(registrationResultPersistenceService.confirmRedisDecision(
                        eq(JOURNAL_ID), eq(reserved), anyLong()))
                .thenReturn(reserved);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(reserved);
        verify(registrationResultPersistenceService)
                .confirmRedisDecision(eq(JOURNAL_ID), eq(reserved), anyLong());
        verify(registrationResultPersistenceService, never())
                .persistJournalWithDatabaseFallback(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Redis 연결 실패를 한 번 재시도한 뒤 DB fallback으로 현재 신청을 완료한다")
    void switchesToDatabaseFallbackAfterRedisConnectionFailure() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult fallback =
                StockReservationResult.reserved(2, UserStatus.SUCCESS, -2, 1);
        stubOpenJournal(journal, EventAdmissionMode.REDIS);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenThrow(new RedisConnectionFailureException("connection refused"));
        when(registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                        eq(JOURNAL_ID), anyLong()))
                .thenReturn(fallback);
        when(eventAdmissionControlService.isDatabaseFallback(EVENT_ID)).thenReturn(true);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(fallback);
        assertThat(coordinator.isDatabaseFallback(EVENT_ID)).isTrue();
        verify(eventAdmissionControlService)
                .activateDatabaseFallback(eq(EVENT_ID), any(RedisConnectionFailureException.class));
        verify(waitingQueueService, times(2))
                .reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH);
        verify(registrationResultPersistenceService)
                .persistJournalWithDatabaseFallback(eq(JOURNAL_ID), anyLong());
    }

    @Test
    @DisplayName("DB fallback 전환은 구버전 요청을 막는 Redis fence를 먼저 기록한다")
    void writesRedisFenceBeforePersistingDatabaseFallbackMode() {
        RuntimeException cause = new RuntimeException("redis state mismatch");

        coordinator.activateDatabaseFallback(EVENT_ID, cause);

        InOrder fenceThenDatabase = inOrder(waitingQueueService, eventAdmissionControlService);
        fenceThenDatabase
                .verify(waitingQueueService)
                .markEventStockClosed(eq(EVENT_ID), any(Duration.class));
        fenceThenDatabase
                .verify(eventAdmissionControlService)
                .activateDatabaseFallback(EVENT_ID, cause);
    }

    @Test
    @DisplayName("첫 Redis 응답만 유실되면 멱등 재시도로 예약을 확정하고 DB fallback은 열지 않는다")
    void confirmsRedisReservationWhenRetrySucceeds() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult reserved =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 2);
        stubOpenJournal(journal, EventAdmissionMode.REDIS);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenThrow(new RedisConnectionFailureException("response lost"))
                .thenReturn(reserved);
        when(registrationResultPersistenceService.confirmRedisDecision(
                        eq(JOURNAL_ID), eq(reserved), anyLong()))
                .thenReturn(reserved);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(reserved);
        verify(waitingQueueService, times(2))
                .reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH);
        verify(eventAdmissionControlService, never()).activateDatabaseFallback(anyLong(), any());
        verify(registrationResultPersistenceService, never())
                .persistJournalWithDatabaseFallback(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Redis 응답이 손상돼 해석할 수 없어도 DB fallback으로 현재 신청을 완료한다")
    void switchesToDatabaseFallbackAfterMalformedRedisResponse() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult fallback =
                StockReservationResult.reserved(2, UserStatus.SUCCESS, -2, 1);
        stubOpenJournal(journal, EventAdmissionMode.REDIS);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenThrow(new IllegalStateException("malformed Redis response"));
        when(registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                        eq(JOURNAL_ID), anyLong()))
                .thenReturn(fallback);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(fallback);
        verify(eventAdmissionControlService)
                .activateDatabaseFallback(eq(EVENT_ID), any(IllegalStateException.class));
    }

    @Test
    @DisplayName("Redis 필수 상태가 유실되면 UNAVAILABLE을 반환하지 않고 DB fallback으로 접수한다")
    void fallsBackWhenRedisStateIsUnavailable() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult fallback =
                StockReservationResult.reserved(2, UserStatus.SUCCESS, -2, 1);
        stubOpenJournal(journal, EventAdmissionMode.REDIS);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenReturn(StockReservationResult.unavailable(null));
        when(registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                        eq(JOURNAL_ID), anyLong()))
                .thenReturn(fallback);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(fallback);
        verify(eventAdmissionControlService)
                .activateDatabaseFallback(eq(EVENT_ID), any(IllegalStateException.class));
        verify(registrationResultPersistenceService)
                .persistJournalWithDatabaseFallback(eq(JOURNAL_ID), anyLong());
    }

    @Test
    @DisplayName("이미 DB fallback인 이벤트는 Redis를 거치지 않고 접수한다")
    void continuesInDatabaseFallbackMode() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult fallback =
                StockReservationResult.reserved(3, UserStatus.PREPARE, 1, 0);
        stubOpenJournal(journal, EventAdmissionMode.DB_FALLBACK);
        when(registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                        eq(JOURNAL_ID), anyLong()))
                .thenReturn(fallback);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(fallback);
        verify(waitingQueueService, never())
                .reserveAndRegisterQueue(
                        any(), any(), any(), anyLong(), any(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("scale-down 서버는 Redis가 없으면 최종 신청을 허용하지 않는다")
    void rejectsFinalAdmissionWhenRedisBeanIsDisabled() {
        ReflectionTestUtils.setField(coordinator, "waitingQueueService", null);

        assertThatThrownBy(() -> coordinator.admit(registration, USER_ID, sector, EVENT_ID))
                .isSameAs(RedisStockUnavailableException.EXCEPTION);

        verify(registrationAdmissionJournalService, never())
                .openJournal(any(), anyLong(), anyLong(), anyLong(), any(), anyLong());
        verify(registrationResultPersistenceService, never())
                .persistJournalWithDatabaseFallback(anyLong(), anyLong());
    }

    @Test
    @DisplayName("정상적인 Redis 재고 소진은 저널에 거절을 기록하고 fallback으로 전환하지 않는다")
    void recordsRejectionWithoutFallback() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult noStock = StockReservationResult.noStock(0);
        stubOpenJournal(journal, EventAdmissionMode.REDIS);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenReturn(noStock);
        when(registrationAdmissionJournalService.rejectRedisDecision(
                        eq(JOURNAL_ID), eq(noStock), anyLong()))
                .thenReturn(noStock);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(noStock);
        verify(registrationAdmissionJournalService)
                .rejectRedisDecision(eq(JOURNAL_ID), eq(noStock), anyLong());
        verify(eventAdmissionControlService, never()).activateDatabaseFallback(anyLong(), any());
        verify(registrationResultPersistenceService, never())
                .confirmRedisDecision(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("DB 이벤트가 OPEN인 Redis CLOSED 응답은 fallback fence로 보고 DB에서 접수한다")
    void treatsClosedRedisStockAsFallbackFenceWhileEventIsOpen() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult closed = StockReservationResult.closed(2);
        StockReservationResult fallback =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 1);
        stubOpenJournal(journal, EventAdmissionMode.REDIS);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenReturn(closed);
        when(eventAdmissionControlService.isOpenForAdmission(EVENT_ID)).thenReturn(true);
        when(registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                        eq(JOURNAL_ID), anyLong()))
                .thenReturn(fallback);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(fallback);
        verify(eventAdmissionControlService)
                .activateDatabaseFallback(eq(EVENT_ID), any(IllegalStateException.class));
        verify(registrationAdmissionJournalService, never())
                .rejectRedisDecision(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("DB 이벤트도 종료된 Redis CLOSED 응답은 정상적인 신청 종료로 기록한다")
    void recordsClosedDecisionWhenDatabaseEventIsClosed() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult closed = StockReservationResult.closed(2);
        stubOpenJournal(journal, EventAdmissionMode.REDIS);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenReturn(closed);
        when(eventAdmissionControlService.isOpenForAdmission(EVENT_ID)).thenReturn(false);
        when(registrationAdmissionJournalService.rejectRedisDecision(
                        eq(JOURNAL_ID), eq(closed), anyLong()))
                .thenReturn(closed);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(closed);
        verify(registrationAdmissionJournalService)
                .rejectRedisDecision(eq(JOURNAL_ID), eq(closed), anyLong());
        verify(eventAdmissionControlService, never()).activateDatabaseFallback(anyLong(), any());
    }

    @Test
    @DisplayName("Redis 거절을 기록하기 전에 epoch가 바뀌면 stale 결과를 버리고 DB로 현재 신청을 확정한다")
    void fallsBackWhenRedisRejectionBecomesStale() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult staleNoStock = StockReservationResult.noStock(0);
        StockReservationResult fallback =
                StockReservationResult.reserved(3, UserStatus.PREPARE, 1, 0);
        stubOpenJournal(journal, EventAdmissionMode.REDIS);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY,
                        REGISTRATION_PAYLOAD,
                        EMAIL,
                        USER_ID,
                        sector,
                        EVENT_ID,
                        JOURNAL_ID,
                        ADMISSION_EPOCH))
                .thenReturn(staleNoStock);
        when(registrationAdmissionJournalService.rejectRedisDecision(
                        eq(JOURNAL_ID), eq(staleNoStock), anyLong()))
                .thenThrow(new AdmissionEpochChangedException(EVENT_ID, JOURNAL_ID));
        when(registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                        eq(JOURNAL_ID), anyLong()))
                .thenReturn(fallback);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(fallback);
        verify(eventAdmissionControlService)
                .activateDatabaseFallback(eq(EVENT_ID), any(AdmissionEpochChangedException.class));
        verify(registrationAdmissionJournalService)
                .persistReceivedInDatabaseFallback(EVENT_ID, JOURNAL_ID);
        verify(registrationResultPersistenceService)
                .persistJournalWithDatabaseFallback(eq(JOURNAL_ID), anyLong());
    }

    @Test
    @DisplayName("이미 확정된 저널은 Redis를 다시 호출하지 않고 기존 결과를 정확히 반환한다")
    void returnsExactResultFromExistingJournal() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal();
        journal.confirm(RegistrationDecisionSource.REDIS, 4, UserStatus.PREPARE, 2, 0, 1_234L);
        StockReservationResult exact = StockReservationResult.reserved(4, UserStatus.PREPARE, 2, 0);
        when(registrationAdmissionJournalService.openJournal(
                        eq(registration),
                        eq(USER_ID),
                        eq(SECTOR_ID),
                        eq(EVENT_ID),
                        eq(REGISTRATION_PAYLOAD),
                        anyLong()))
                .thenReturn(attempt(journal, EventAdmissionMode.REDIS, true));
        when(registrationAdmissionJournalService.toResult(journal)).thenReturn(exact);

        StockReservationResult result = coordinator.admit(registration, USER_ID, sector, EVENT_ID);

        assertThat(result).isSameAs(exact);
        verify(waitingQueueService, never())
                .reserveAndRegisterQueue(
                        any(), any(), any(), anyLong(), any(), anyLong(), anyLong(), anyLong());
        verify(registrationResultPersistenceService, never())
                .confirmRedisDecision(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("DB fallback 이벤트를 복구 대상으로 제공하고 확정 저널을 본 저장한다")
    void materializesConfirmedJournalsDuringRecovery() {
        when(eventAdmissionControlService.fallbackEventIds()).thenReturn(Set.of(EVENT_ID));

        assertThat(coordinator.recoveryEventIds()).containsExactly(EVENT_ID);
        assertThat(coordinator.recover(EVENT_ID)).isTrue();

        verify(registrationAdmissionJournalService).materializeMissingRegistrations(EVENT_ID);
    }

    @Test
    @DisplayName("서버 재기동 때 Redis와 이벤트 초기화 키가 정상이면 Redis 모드를 유지한다")
    void keepsRedisAdmissionOnHealthyStartup() {
        when(waitingQueueService.isAvailable()).thenReturn(true);
        when(waitingQueueService.isEventStockInitialized(EVENT_ID)).thenReturn(true);

        coordinator.restoreOpenEventAdmission(EVENT_ID);

        verify(eventAdmissionControlService, never()).activateDatabaseFallback(anyLong(), any());
        verify(registrationAdmissionJournalService).materializeMissingRegistrations(EVENT_ID);
    }

    @Test
    @DisplayName("서버 재기동 때 Redis가 응답하지 않으면 DB fallback으로 전환한다")
    void activatesDatabaseFallbackWhenRedisIsUnavailableOnStartup() {
        when(waitingQueueService.isAvailable()).thenReturn(false);

        coordinator.restoreOpenEventAdmission(EVENT_ID);

        verify(eventAdmissionControlService)
                .activateDatabaseFallback(eq(EVENT_ID), any(IllegalStateException.class));
        verify(waitingQueueService, never()).isEventStockInitialized(EVENT_ID);
        verify(registrationAdmissionJournalService).materializeMissingRegistrations(EVENT_ID);
    }

    @Test
    @DisplayName("서버 재기동 때 이벤트 초기화 키가 유실됐으면 DB fallback으로 전환한다")
    void activatesDatabaseFallbackWhenInitializationKeyIsMissingOnStartup() {
        when(waitingQueueService.isAvailable()).thenReturn(true);
        when(waitingQueueService.isEventStockInitialized(EVENT_ID)).thenReturn(false);

        coordinator.restoreOpenEventAdmission(EVENT_ID);

        verify(eventAdmissionControlService)
                .activateDatabaseFallback(eq(EVENT_ID), any(IllegalStateException.class));
        verify(registrationAdmissionJournalService).materializeMissingRegistrations(EVENT_ID);
    }

    private void stubOpenJournal(
            RegistrationAdmissionJournal journal, EventAdmissionMode admissionMode) {
        when(registrationAdmissionJournalService.openJournal(
                        eq(registration),
                        eq(USER_ID),
                        eq(SECTOR_ID),
                        eq(EVENT_ID),
                        eq(REGISTRATION_PAYLOAD),
                        anyLong()))
                .thenReturn(attempt(journal, admissionMode, false));
    }

    private RegistrationAdmissionJournalService.AdmissionAttempt attempt(
            RegistrationAdmissionJournal journal,
            EventAdmissionMode admissionMode,
            boolean existing) {
        return new RegistrationAdmissionJournalService.AdmissionAttempt(
                journal, admissionMode, existing);
    }

    private RegistrationAdmissionJournal receivedJournal() {
        RegistrationAdmissionJournal journal =
                RegistrationAdmissionJournal.received(
                        EVENT_ID,
                        SECTOR_ID,
                        USER_ID,
                        EMAIL,
                        ADMISSION_EPOCH,
                        REGISTRATION_PAYLOAD,
                        1_000L);
        ReflectionTestUtils.setField(journal, "id", JOURNAL_ID);
        return journal;
    }
}
