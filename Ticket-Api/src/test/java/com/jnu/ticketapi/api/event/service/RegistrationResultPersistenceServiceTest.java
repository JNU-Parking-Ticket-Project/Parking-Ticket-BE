package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.api.event.service.RegistrationResultPersistenceService.StreamDecisionAction;
import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotOpenEventStatusException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdmissionJournalAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionJournal;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionState;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationDecisionSource;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationResultPersistenceServiceTest {

    @Mock private RegistrationAdaptor registrationAdaptor;
    @Mock private UserAdaptor userAdaptor;
    @Mock private EmailOutboxAdaptor emailOutboxAdaptor;
    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private RegistrationAdmissionJournalAdaptor admissionJournalAdaptor;
    @Mock private EventAdaptor eventAdaptor;
    @Mock private ObjectMapper objectMapper;

    private RegistrationResultPersistenceService service;

    @BeforeEach
    void setUp() {
        service =
                new RegistrationResultPersistenceService(
                        registrationAdaptor,
                        userAdaptor,
                        emailOutboxAdaptor,
                        sectorAdaptor,
                        admissionJournalAdaptor,
                        eventAdaptor,
                        objectMapper);
    }

    @Test
    @DisplayName("Redis 결정은 전체 신청 저장 없이 저널만 확정한다")
    void confirmsRedisDecisionBeforeResponse() {
        Event event = redisEvent();
        RegistrationAdmissionJournal journal = receivedJournal(event.getAdmissionEpoch());
        StockReservationResult reservation = reserved(1, 2);
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);
        when(sectorAdaptor.findById(20L)).thenReturn(sector());

        StockReservationResult result = service.confirmRedisDecision(100L, reservation, 1_234L);

        assertThat(result).isSameAs(reservation);
        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.DECIDED);
        assertThat(journal.getDecisionSource()).isEqualTo(RegistrationDecisionSource.REDIS);
        assertThat(journal.getPosition()).isEqualTo(1);
        verify(admissionJournalAdaptor).saveAndFlush(journal);
        verify(registrationAdaptor, never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any(Registration.class));
        verify(emailOutboxAdaptor, never())
                .saveRegistrationResultIfAbsent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("RECEIVED 저널을 먼저 본 Stream consumer는 결정만 기록하고 본 저장을 미룬다")
    void recordsReceivedStreamDecisionWithoutMaterializing() {
        Event event = redisEvent();
        RegistrationAdmissionJournal journal = receivedJournal(event.getAdmissionEpoch());
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);
        when(sectorAdaptor.findById(20L)).thenReturn(sector());

        StreamDecisionAction action =
                service.recordStreamDecision(
                        100L, event.getAdmissionEpoch(), reserved(1, 2), 1_234L);

        assertThat(action).isEqualTo(StreamDecisionAction.DEFER_MATERIALIZATION);
        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.DECIDED);
        assertThat(journal.getDecisionSource()).isEqualTo(RegistrationDecisionSource.REDIS);
        verify(admissionJournalAdaptor).saveAndFlush(journal);
        verify(registrationAdaptor, never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any(Registration.class));
        verify(emailOutboxAdaptor, never())
                .saveRegistrationResultIfAbsent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("DB fallback 전환 뒤에는 이전 Redis epoch 결정을 확정하지 않는다")
    void fencesRedisDecisionAfterFallbackTransition() {
        Event event = databaseFallbackEvent();
        RegistrationAdmissionJournal journal = receivedJournal(1L);
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);

        assertThatThrownBy(() -> service.confirmRedisDecision(100L, reserved(1, 2), 1_234L))
                .isInstanceOf(AdmissionEpochChangedException.class);

        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.RECEIVED);
        verify(admissionJournalAdaptor, never()).saveAndFlush(journal);
    }

    @Test
    @DisplayName("DB fallback은 확정 저널의 마지막 순번 다음부터 접수를 이어간다")
    void persistsFallbackAfterJournalHighWaterMark() throws Exception {
        Event event = databaseFallbackEvent();
        RegistrationAdmissionJournal journal = receivedJournal(1L);
        Registration registration = registration();
        registration.setId(500L);
        User user = user();
        Sector sector = sector();
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);
        when(userAdaptor.findByIdForUpdate(30L)).thenReturn(user);
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.empty());
        when(admissionJournalAdaptor.findDecidedPositionsBySectorId(20L)).thenReturn(List.of(1));
        when(registrationAdaptor.findSavedPositionsBySectorId(20L)).thenReturn(List.of());
        when(objectMapper.readValue("payload", Registration.class)).thenReturn(registration);
        when(registrationAdaptor.saveAndFlush(registration)).thenReturn(registration);

        StockReservationResult result = service.persistJournalWithDatabaseFallback(100L, 2_345L);

        assertThat(result.getPosition()).isEqualTo(2);
        assertThat(result.getRemainingAmount()).isEqualTo(1);
        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.MATERIALIZED);
        assertThat(journal.getDecisionSource()).isEqualTo(RegistrationDecisionSource.DATABASE);
        assertThat(journal.getRegistrationId()).isEqualTo(500L);
        assertThat(sector.getRemainingAmount()).isEqualTo(1);
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    @Test
    @DisplayName("DB fallback은 확정 순번 사이의 가장 작은 빈 자리를 복구한다")
    void persistsFallbackIntoSmallestAvailablePosition() throws Exception {
        Event event = databaseFallbackEvent();
        RegistrationAdmissionJournal journal = receivedJournal(1L);
        Registration registration = registration();
        registration.setId(500L);
        User user = user();
        Sector sector = sector(1, 1);
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);
        when(userAdaptor.findByIdForUpdate(30L)).thenReturn(user);
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.empty());
        when(admissionJournalAdaptor.findDecidedPositionsBySectorId(20L)).thenReturn(List.of(2));
        when(registrationAdaptor.findSavedPositionsBySectorId(20L)).thenReturn(List.of());
        when(objectMapper.readValue("payload", Registration.class)).thenReturn(registration);
        when(registrationAdaptor.saveAndFlush(registration)).thenReturn(registration);

        StockReservationResult result = service.persistJournalWithDatabaseFallback(100L, 2_345L);

        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(result.getResultStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(result.getSequence()).isEqualTo(-2);
        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.MATERIALIZED);
        assertThat(journal.getPosition()).isEqualTo(1);
        assertThat(journal.getDecisionSource()).isEqualTo(RegistrationDecisionSource.DATABASE);
    }

    @Test
    @DisplayName("DB fallback 재고가 소진되면 저널에 거절 결과를 남겨 재시도를 멱등하게 처리한다")
    void recordsNoStockInFallbackJournal() throws Exception {
        Event event = databaseFallbackEvent();
        RegistrationAdmissionJournal journal = receivedJournal(1L);
        Registration registration = registration();
        User user = user();
        Sector sector = sector();
        sector.syncRemainingAmount(0);
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);
        when(userAdaptor.findByIdForUpdate(30L)).thenReturn(user);
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);
        when(objectMapper.readValue("payload", Registration.class)).thenReturn(registration);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.empty());
        when(admissionJournalAdaptor.findDecidedPositionsBySectorId(20L))
                .thenReturn(List.of(1, 2, 3));
        when(registrationAdaptor.findSavedPositionsBySectorId(20L)).thenReturn(List.of());

        StockReservationResult result = service.persistJournalWithDatabaseFallback(100L, 2_345L);

        assertThat(result.isNoStock()).isTrue();
        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.REJECTED);
        assertThat(journal.getDecisionReason()).isEqualTo("NO_STOCK");
        verify(registrationAdaptor, never()).saveAndFlush(registration);
    }

    @Test
    @DisplayName("이벤트 종료 뒤에도 Redis 확정 저널과 신청서, Outbox를 함께 본 저장한다")
    void materializesConfirmedRedisJournal() throws Exception {
        RegistrationAdmissionJournal journal = receivedJournal(1L);
        journal.confirm(RegistrationDecisionSource.REDIS, 1, UserStatus.SUCCESS, -2, 2, 1_000L);
        Registration registration = registration();
        registration.setId(500L);
        User user = user();
        Sector sector = sector();
        ReflectionTestUtils.setField(sector.getEvent(), "eventStatus", EventStatus.CLOSED);
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(userAdaptor.findByIdForUpdate(30L)).thenReturn(user);
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.empty());
        when(registrationAdaptor.findSavedBySectorIdAndPosition(20L, 1))
                .thenReturn(Optional.empty());
        when(objectMapper.readValue("payload", Registration.class)).thenReturn(registration);
        when(registrationAdaptor.saveAndFlush(registration)).thenReturn(registration);

        StreamDecisionAction action =
                service.recordStreamDecision(100L, 1L, reserved(1, 2), 2_345L);
        StockReservationResult result = service.materializeConfirmedJournal(100L);

        assertThat(action).isEqualTo(StreamDecisionAction.MATERIALIZE);
        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.MATERIALIZED);
        assertThat(registration.isSaved()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(sector.getRemainingAmount()).isEqualTo(2);
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    @Test
    @DisplayName("fallback 전환 뒤 도착한 미확정 Stream 결정은 DB fallback으로 넘긴다")
    void routesStaleStreamDecisionToDatabaseFallback() {
        Event event = databaseFallbackEvent();
        RegistrationAdmissionJournal journal = receivedJournal(1L);
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);

        StreamDecisionAction action =
                service.recordStreamDecision(100L, 1L, reserved(1, 2), 2_345L);

        assertThat(action).isEqualTo(StreamDecisionAction.DATABASE_FALLBACK);
        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.RECEIVED);
        verify(registrationAdaptor, never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any(Registration.class));
    }

    @Test
    @DisplayName("현재 Redis 모드와 다른 epoch의 Stream 결정은 ACK만 허용한다")
    void acknowledgesStreamDecisionFromStaleEpoch() {
        Event event = redisEvent();
        RegistrationAdmissionJournal journal = receivedJournal(event.getAdmissionEpoch());
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);

        StreamDecisionAction action =
                service.recordStreamDecision(
                        100L, event.getAdmissionEpoch() - 1, reserved(1, 2), 2_345L);

        assertThat(action).isEqualTo(StreamDecisionAction.ACK_ONLY);
        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.RECEIVED);
        verify(admissionJournalAdaptor, never()).saveAndFlush(journal);
    }

    @Test
    @DisplayName("이미 본 저장된 저널은 Stream 재전달 시 ACK만 수행한다")
    void acknowledgesMaterializedJournalIdempotently() {
        RegistrationAdmissionJournal journal = receivedJournal(1L);
        journal.confirm(RegistrationDecisionSource.REDIS, 1, UserStatus.SUCCESS, -2, 2, 1_000L);
        journal.markMaterialized(500L, 1_100L);
        when(admissionJournalAdaptor.findByIdForUpdate(100L)).thenReturn(journal);

        StreamDecisionAction action =
                service.recordStreamDecision(100L, 1L, reserved(1, 2), 2_345L);

        assertThat(action).isEqualTo(StreamDecisionAction.ACK_ONLY);
        verify(registrationAdaptor, never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any(Registration.class));
    }

    @Test
    @DisplayName("기존 Redis 예약 저장 경로도 구간 체크포인트와 Outbox를 함께 갱신한다")
    void persistsLegacyRedisDecisionAndCheckpoint() {
        Event event = redisEvent();
        Registration registration = registration();
        registration.setId(500L);
        User user = user();
        Sector sector = sector();
        when(eventAdaptor.findByIdForAdmissionRead(10L)).thenReturn(event);
        when(userAdaptor.findByIdForUpdate(30L)).thenReturn(user);
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.empty());
        when(registrationAdaptor.findSavedBySectorIdAndPosition(20L, 1))
                .thenReturn(Optional.empty());
        when(registrationAdaptor.saveAndFlush(registration)).thenReturn(registration);

        StockReservationResult result =
                service.persistRedisReservation(
                        registration, 30L, 20L, 10L, reserved(1, 2), 1_234L);

        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(registration.isSaved()).isTrue();
        assertThat(sector.getRemainingAmount()).isEqualTo(2);
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    @Test
    @DisplayName("Redis 복구 스냅샷은 DB position과 체크포인트 중 더 진행된 값을 보존한다")
    void preparesRecoverySnapshotWithoutRewindingPosition() {
        Sector sector = sector();
        Registration saved = registration();
        saved.finalSave(2, UserStatus.SUCCESS, -2);
        saved.setSector(sector);
        when(registrationAdaptor.findSavedForAdmissionRecovery(10L))
                .thenReturn(List.of(saved));
        when(sectorAdaptor.findByEventId(10L)).thenReturn(List.of(sector));
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);

        EventStockRecoverySnapshot snapshot = service.prepareRecoverySnapshot(10L);

        assertThat(snapshot.sectors()).containsExactly(sector);
        assertThat(snapshot.reservedEmails()).containsExactly("student@jnu.ac.kr");
        assertThat(sector.getRemainingAmount()).isEqualTo(1);
    }

    @Test
    @DisplayName("CLOSED 이벤트는 Redis 접수 상태를 다시 만들지 않는다")
    void rejectsRecoverySnapshotForClosedEvent() {
        Sector sector = sector();
        ReflectionTestUtils.setField(sector.getEvent(), "eventStatus", EventStatus.CLOSED);
        when(registrationAdaptor.findSavedForAdmissionRecovery(10L)).thenReturn(List.of());
        when(sectorAdaptor.findByEventId(10L)).thenReturn(List.of(sector));
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);

        assertThatThrownBy(() -> service.prepareRecoverySnapshot(10L))
                .isSameAs(NotOpenEventStatusException.EXCEPTION);
    }

    private RegistrationAdmissionJournal receivedJournal(Long admissionEpoch) {
        RegistrationAdmissionJournal journal =
                RegistrationAdmissionJournal.received(
                        10L, 20L, 30L, "student@jnu.ac.kr", admissionEpoch, "payload", 900L);
        ReflectionTestUtils.setField(journal, "id", 100L);
        return journal;
    }

    private StockReservationResult reserved(int position, int remainingAmount) {
        return StockReservationResult.reserved(position, UserStatus.SUCCESS, -2, remainingAmount);
    }

    private Event redisEvent() {
        Event event = event();
        event.initializeRedisAdmission();
        return event;
    }

    private Event databaseFallbackEvent() {
        Event event = redisEvent();
        event.activateDatabaseAdmissionFallback();
        return event;
    }

    private Event event() {
        Event event = Event.builder().title("주차권").sector(java.util.List.of()).build();
        ReflectionTestUtils.setField(event, "id", 10L);
        ReflectionTestUtils.setField(event, "eventStatus", EventStatus.OPEN);
        return event;
    }

    private Sector sector() {
        return sector(2, 1);
    }

    private Sector sector(int sectorCapacity, int reserve) {
        Sector sector =
                Sector.builder()
                        .sectorNumber("1구간")
                        .name("공과대학")
                        .sectorCapacity(sectorCapacity)
                        .reserve(reserve)
                        .build();
        ReflectionTestUtils.setField(sector, "id", 20L);
        sector.setEvent(event());
        return sector;
    }

    private User user() {
        return User.builder()
                .email("student@jnu.ac.kr")
                .pwd("encoded-password")
                .userRole(UserRole.USER)
                .build();
    }

    private Registration registration() {
        return Registration.builder()
                .email("student@jnu.ac.kr")
                .name("학생")
                .studentNum("20240001")
                .affiliation("공과대학")
                .department("컴퓨터공학과")
                .carNum("12가3456")
                .isLight(false)
                .phoneNum("010-0000-0000")
                .createdAt(LocalDateTime.of(2026, 7, 31, 10, 0))
                .isSaved(false)
                .eventId(10L)
                .build();
    }
}
