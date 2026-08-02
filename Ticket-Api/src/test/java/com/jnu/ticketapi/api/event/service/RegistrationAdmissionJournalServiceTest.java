package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.NotOpenEventStatusException;
import com.jnu.ticketdomain.domains.events.exception.NotPublishEventException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdmissionJournalAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionJournal;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionState;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationDecisionSource;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.RegistrationPayloadConverter;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationAdmissionJournalServiceTest {

    private static final long EVENT_ID = 10L;
    private static final long SECTOR_ID = 20L;
    private static final long USER_ID = 30L;
    private static final long JOURNAL_ID = 40L;
    private static final long ADMISSION_EPOCH = 7L;
    private static final String EMAIL = "student@jnu.ac.kr";
    private static final String PAYLOAD =
            "{\"email\":\"student@jnu.ac.kr\",\"name\":\"홍길동\",\"studentNum\":\"183027\","
                    + "\"affiliation\":\"공과대학\",\"department\":\"컴퓨터정보통신공학과\","
                    + "\"carNum\":\"12가3456\",\"phoneNum\":\"010-1111-2222\","
                    + "\"isLight\":true,\"eventId\":10}";

    @Mock private RegistrationAdmissionJournalAdaptor admissionJournalAdaptor;
    @Mock private RegistrationResultPersistenceService registrationResultPersistenceService;
    @Mock private EventAdaptor eventAdaptor;
    @Mock private RegistrationAdaptor registrationAdaptor;
    @Mock private UserAdaptor userAdaptor;
    @Mock private Event event;
    @Mock private Registration registration;

    private RegistrationAdmissionJournalService service;

    @BeforeEach
    void setUp() {
        service =
                new RegistrationAdmissionJournalService(
                        admissionJournalAdaptor,
                        registrationResultPersistenceService,
                        eventAdaptor,
                        registrationAdaptor,
                        userAdaptor);
        org.mockito.Mockito.lenient()
                .when(eventAdaptor.findByIdForAdmissionRead(EVENT_ID))
                .thenReturn(event);
        org.mockito.Mockito.lenient()
                .when(event.getAdmissionMode())
                .thenReturn(EventAdmissionMode.REDIS);
        org.mockito.Mockito.lenient().when(event.getAdmissionEpoch()).thenReturn(ADMISSION_EPOCH);
        org.mockito.Mockito.lenient().when(event.getEventStatus()).thenReturn(EventStatus.OPEN);
        org.mockito.Mockito.lenient().when(event.getPublish()).thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(event.isRedisAdmission(ADMISSION_EPOCH))
                .thenReturn(true);
        org.mockito.Mockito.lenient().when(registration.getEmail()).thenReturn(EMAIL);
    }

    @Test
    @DisplayName("Redis 호출 전에 복구 payload와 admission epoch를 가진 RECEIVED 저널을 저장한다")
    void opensReceivedJournalBeforeRedisAdmission() {
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.empty());
        when(admissionJournalAdaptor.saveAndFlush(any(RegistrationAdmissionJournal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationAdmissionJournalService.AdmissionAttempt attempt =
                service.openJournal(registration, USER_ID, SECTOR_ID, EVENT_ID, PAYLOAD, 1_234L);

        assertThat(attempt.existing()).isFalse();
        assertThat(attempt.admissionMode()).isEqualTo(EventAdmissionMode.REDIS);
        assertThat(attempt.journal().getState()).isEqualTo(RegistrationAdmissionState.RECEIVED);
        assertThat(attempt.journal().getAdmissionEpoch()).isEqualTo(ADMISSION_EPOCH);
        assertThat(attempt.journal().getRegistrationPayload()).isEqualTo(PAYLOAD);
        ArgumentCaptor<RegistrationAdmissionJournal> captor =
                ArgumentCaptor.forClass(RegistrationAdmissionJournal.class);
        verify(admissionJournalAdaptor).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(captor.getValue().getSectorId()).isEqualTo(SECTOR_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("이벤트 종료가 확정된 뒤에는 새 RECEIVED 저널을 만들지 않는다")
    void rejectsNewJournalAfterEventWasClosed() {
        when(event.getEventStatus()).thenReturn(EventStatus.CLOSED);
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.openJournal(
                                        registration,
                                        USER_ID,
                                        SECTOR_ID,
                                        EVENT_ID,
                                        PAYLOAD,
                                        1_234L))
                .isSameAs(NotOpenEventStatusException.EXCEPTION);

        verify(admissionJournalAdaptor, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("이벤트가 비공개로 전환된 뒤에는 새 RECEIVED 저널을 만들지 않는다")
    void rejectsNewJournalAfterEventWasUnpublished() {
        when(event.getPublish()).thenReturn(false);
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.openJournal(
                                        registration,
                                        USER_ID,
                                        SECTOR_ID,
                                        EVENT_ID,
                                        PAYLOAD,
                                        1_234L))
                .isSameAs(NotPublishEventException.EXCEPTION);

        verify(admissionJournalAdaptor, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동일 이벤트와 이메일의 기존 저널은 새로 만들지 않고 그대로 반환한다")
    void returnsExistingJournalWithoutDuplicateInsert() {
        RegistrationAdmissionJournal journal = decidedJournal();
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.of(journal));

        RegistrationAdmissionJournalService.AdmissionAttempt attempt =
                service.openJournal(registration, USER_ID, SECTOR_ID, EVENT_ID, PAYLOAD, 1_234L);

        assertThat(attempt.existing()).isTrue();
        assertThat(attempt.hasResult()).isTrue();
        assertThat(attempt.journal()).isSameAs(journal);
        verify(admissionJournalAdaptor, never()).saveAndFlush(any());
        verify(event, never()).getEventStatus();
        verify(event, never()).getPublish();
    }

    @Test
    @DisplayName("같은 이메일이 기존 저널과 다른 구간으로 재요청하면 기존 결정을 재사용하지 않는다")
    void rejectsExistingJournalForDifferentSector() {
        RegistrationAdmissionJournal journal = receivedJournal();
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.of(journal));

        assertThatThrownBy(
                        () ->
                                service.openJournal(
                                        registration, USER_ID, 999L, EVENT_ID, PAYLOAD, 1_234L))
                .isSameAs(AlreadyExistRegistrationException.EXCEPTION);

        verify(admissionJournalAdaptor, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("저널 생성 전에 신청자와 임시저장 행을 잠그고 새로 발견한 행의 식별자를 payload에 고정한다")
    void freezesConcurrentTemporaryRegistrationBeforeOpeningJournal() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 2, 10, 0);
        Registration finalRegistration = registration("12가3456");
        Registration temporaryRegistration = registration("임시차량");
        temporaryRegistration.setId(55L);
        temporaryRegistration.setCreatedAt(createdAt);
        when(registrationAdaptor.findTemporaryByEmailAndEventIdForUpdate(EMAIL, EVENT_ID))
                .thenReturn(Optional.of(temporaryRegistration));
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.empty());
        when(admissionJournalAdaptor.saveAndFlush(any(RegistrationAdmissionJournal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationAdmissionJournalService.AdmissionAttempt attempt =
                service.openJournal(
                        finalRegistration,
                        USER_ID,
                        SECTOR_ID,
                        EVENT_ID,
                        "stale-payload-without-temp-id",
                        1_234L);

        assertThat(finalRegistration.getId()).isEqualTo(55L);
        assertThat(finalRegistration.getCreatedAt()).isEqualTo(createdAt);
        assertThat(attempt.journal().getRegistrationPayload())
                .isEqualTo(RegistrationPayloadConverter.toJson(finalRegistration));
        InOrder locksThenJournal =
                inOrder(userAdaptor, registrationAdaptor, admissionJournalAdaptor);
        locksThenJournal.verify(userAdaptor).findByIdForUpdate(USER_ID);
        locksThenJournal
                .verify(registrationAdaptor)
                .findTemporaryByEmailAndEventIdForUpdate(EMAIL, EVENT_ID);
        locksThenJournal.verify(admissionJournalAdaptor).findByEventIdAndEmail(EVENT_ID, EMAIL);
    }

    @Test
    @DisplayName("임시저장은 신청자와 기존 임시 행을 잠근 뒤 최종 신청 저널을 다시 확인한다")
    void rejectsTemporarySaveAfterAdmissionJournalWasOpened() {
        Registration temporaryRegistration = registration("임시차량");
        when(registrationAdaptor.findTemporaryByEmailAndEventIdForUpdate(EMAIL, EVENT_ID))
                .thenReturn(Optional.of(temporaryRegistration));
        when(admissionJournalAdaptor.existsByEventIdAndEmail(EVENT_ID, EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.lockForTemporarySave(USER_ID, EVENT_ID, EMAIL))
                .isSameAs(AlreadyExistRegistrationException.EXCEPTION);

        InOrder locksThenJournal =
                inOrder(userAdaptor, registrationAdaptor, admissionJournalAdaptor);
        locksThenJournal.verify(userAdaptor).findByIdForUpdate(USER_ID);
        locksThenJournal
                .verify(registrationAdaptor)
                .findTemporaryByEmailAndEventIdForUpdate(EMAIL, EVENT_ID);
        locksThenJournal.verify(admissionJournalAdaptor).existsByEventIdAndEmail(EVENT_ID, EMAIL);
    }

    @Test
    @DisplayName("응답이 유실된 동일 신청은 확정 저널의 원래 결과를 반환한다")
    void restoresCompletedReservationForSameBusinessRequest() {
        Registration retryRegistration = registration("12가3456");
        RegistrationAdmissionJournal journal =
                RegistrationAdmissionJournal.received(
                        EVENT_ID,
                        SECTOR_ID,
                        USER_ID,
                        EMAIL,
                        ADMISSION_EPOCH,
                        RegistrationPayloadConverter.toJson(retryRegistration),
                        1_000L);
        journal.confirm(RegistrationDecisionSource.REDIS, 4, UserStatus.PREPARE, 2, 0, 1_500L);
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.of(journal));

        Optional<RegistrationAdmissionJournalService.ExistingAdmission> restored =
                service.findExistingAdmission(retryRegistration, USER_ID, SECTOR_ID, EVENT_ID);

        assertThat(restored).isPresent();
        assertThat(restored.orElseThrow().accepted()).isTrue();
    }

    @Test
    @DisplayName("기존 확정 저널과 신청 내용이 다르면 응답 유실 재시도로 취급하지 않는다")
    void doesNotRestoreCompletedReservationForDifferentBusinessRequest() {
        Registration originalRegistration = registration("12가3456");
        Registration changedRegistration = registration("99나9999");
        RegistrationAdmissionJournal journal =
                RegistrationAdmissionJournal.received(
                        EVENT_ID,
                        SECTOR_ID,
                        USER_ID,
                        EMAIL,
                        ADMISSION_EPOCH,
                        RegistrationPayloadConverter.toJson(originalRegistration),
                        1_000L);
        journal.confirm(RegistrationDecisionSource.REDIS, 4, UserStatus.PREPARE, 2, 0, 1_500L);
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.of(journal));

        Optional<RegistrationAdmissionJournalService.ExistingAdmission> restored =
                service.findExistingAdmission(changedRegistration, USER_ID, SECTOR_ID, EVENT_ID);

        assertThat(restored).isEmpty();
    }

    @Test
    @DisplayName("동일한 RECEIVED 신청 재시도는 캡차 재검증 없이 기존 저널을 재개한다")
    void resumesReceivedJournalForSameBusinessRequest() {
        Registration retryRegistration = registration("12가3456");
        RegistrationAdmissionJournal journal =
                RegistrationAdmissionJournal.received(
                        EVENT_ID,
                        SECTOR_ID,
                        USER_ID,
                        EMAIL,
                        ADMISSION_EPOCH,
                        RegistrationPayloadConverter.toJson(retryRegistration),
                        1_000L);
        when(admissionJournalAdaptor.findByEventIdAndEmail(EVENT_ID, EMAIL))
                .thenReturn(Optional.of(journal));

        Optional<RegistrationAdmissionJournalService.ExistingAdmission> restored =
                service.findExistingAdmission(retryRegistration, USER_ID, SECTOR_ID, EVENT_ID);

        assertThat(restored).isPresent();
        assertThat(restored.orElseThrow().accepted()).isFalse();
    }

    @Test
    @DisplayName("확정된 기존 저널에서 위치와 결과를 그대로 복원한다")
    void restoresExactResultFromDecidedJournal() {
        RegistrationAdmissionJournal journal = decidedJournal();

        StockReservationResult result = service.toResult(journal);

        assertThat(result.isReserved()).isTrue();
        assertThat(result.getPosition()).isEqualTo(4);
        assertThat(result.getResultStatus()).isEqualTo(UserStatus.PREPARE);
        assertThat(result.getSequence()).isEqualTo(2);
        assertThat(result.getRemainingAmount()).isZero();
    }

    @Test
    @DisplayName("정상적인 Redis 거절 결과는 잠근 RECEIVED 저널에 기록한다")
    void recordsRejectedDecision() {
        RegistrationAdmissionJournal journal = receivedJournal();
        StockReservationResult noStock = StockReservationResult.noStock(0);
        when(admissionJournalAdaptor.findByIdForUpdate(JOURNAL_ID)).thenReturn(journal);

        StockReservationResult result = service.rejectRedisDecision(JOURNAL_ID, noStock, 2_000L);

        assertThat(result).isSameAs(noStock);
        assertThat(journal.isRejected()).isTrue();
        assertThat(journal.getDecisionReason()).isEqualTo("NO_STOCK");
        StockReservationResult restored = service.toResult(journal);
        assertThat(restored.isNoStock()).isTrue();
        assertThat(restored.getRemainingAmount()).isZero();
    }

    @Test
    @DisplayName("이전 admission epoch의 Redis 거절 결과는 저널에 기록하지 않는다")
    void rejectsStaleEpochRedisRejection() {
        RegistrationAdmissionJournal journal = receivedJournal();
        when(admissionJournalAdaptor.findByIdForUpdate(JOURNAL_ID)).thenReturn(journal);
        when(event.isRedisAdmission(ADMISSION_EPOCH)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.rejectRedisDecision(
                                        JOURNAL_ID, StockReservationResult.noStock(0), 2_000L))
                .isInstanceOf(AdmissionEpochChangedException.class);

        assertThat(journal.getState()).isEqualTo(RegistrationAdmissionState.RECEIVED);
        assertThat(journal.getDecisionReason()).isNull();
    }

    @Test
    @DisplayName("Stream이 유실된 확정 저널은 저장 서비스에 본 신청 복구를 위임한다")
    void materializesMissingRegistrationFromDecidedJournal() {
        RegistrationAdmissionJournal journal = decidedJournal();
        when(admissionJournalAdaptor.findDecidedByEventId(eq(EVENT_ID), anyLong()))
                .thenReturn(List.of(journal));

        service.materializeMissingRegistrations(EVENT_ID);

        verify(admissionJournalAdaptor).findDecidedByEventId(eq(EVENT_ID), anyLong());
        verify(registrationResultPersistenceService).materializeConfirmedJournal(JOURNAL_ID);
    }

    @Test
    @DisplayName("DB fallback 복구는 앞선 RECEIVED 저장이 실패하면 뒤 순번으로 넘어가지 않는다")
    void stopsFallbackRecoveryWhenEarlierJournalFails() {
        RegistrationAdmissionJournal first = receivedJournal();
        RegistrationAdmissionJournal second = receivedJournal();
        ReflectionTestUtils.setField(second, "id", JOURNAL_ID + 1);
        when(admissionJournalAdaptor.findReceivedThrough(EVENT_ID, Long.MAX_VALUE))
                .thenReturn(List.of(first, second));
        when(registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                        eq(JOURNAL_ID), anyLong()))
                .thenThrow(new IllegalStateException("temporary DB failure"));

        service.recoverReceivedInDatabaseFallback(EVENT_ID);

        verify(registrationResultPersistenceService)
                .persistJournalWithDatabaseFallback(eq(JOURNAL_ID), anyLong());
        verify(registrationResultPersistenceService, never())
                .persistJournalWithDatabaseFallback(eq(JOURNAL_ID + 1), anyLong());
    }

    @Test
    @DisplayName("Redis가 없는 서버에서도 확정 저널을 배치로 찾아 본 저장한다")
    void materializesGlobalDecidedJournalBatch() {
        RegistrationAdmissionJournal journal = decidedJournal();
        when(admissionJournalAdaptor.findMaxDecidedId(anyLong())).thenReturn(JOURNAL_ID);
        when(admissionJournalAdaptor.findDecidedBatch(anyLong(), eq(0L), eq(JOURNAL_ID)))
                .thenReturn(List.of(journal));

        service.materializeMissingRegistrations();

        verify(admissionJournalAdaptor).findDecidedBatch(anyLong(), eq(0L), eq(JOURNAL_ID));
        verify(registrationResultPersistenceService).materializeConfirmedJournal(JOURNAL_ID);
    }

    @Test
    @DisplayName("전역 복구 배치는 고정한 상한 안에서 마지막으로 확인한 저널 다음부터 조회한다")
    void resumesGlobalMaterializationAfterLastJournalCursor() {
        RegistrationAdmissionJournal journal = decidedJournal();
        long upperBound = 100L;
        when(admissionJournalAdaptor.findMaxDecidedId(anyLong())).thenReturn(upperBound);
        when(admissionJournalAdaptor.findDecidedBatch(anyLong(), eq(0L), eq(upperBound)))
                .thenReturn(List.of(journal));
        when(admissionJournalAdaptor.findDecidedBatch(anyLong(), eq(JOURNAL_ID), eq(upperBound)))
                .thenReturn(List.of());

        service.materializeMissingRegistrations();
        service.materializeMissingRegistrations();

        verify(admissionJournalAdaptor).findDecidedBatch(anyLong(), eq(JOURNAL_ID), eq(upperBound));
    }

    private RegistrationAdmissionJournal decidedJournal() {
        RegistrationAdmissionJournal journal = receivedJournal();
        journal.confirm(RegistrationDecisionSource.REDIS, 4, UserStatus.PREPARE, 2, 0, 1_500L);
        return journal;
    }

    private RegistrationAdmissionJournal receivedJournal() {
        RegistrationAdmissionJournal journal =
                RegistrationAdmissionJournal.received(
                        EVENT_ID, SECTOR_ID, USER_ID, EMAIL, ADMISSION_EPOCH, PAYLOAD, 1_000L);
        ReflectionTestUtils.setField(journal, "id", JOURNAL_ID);
        return journal;
    }

    private Registration registration(String carNumber) {
        return Registration.builder()
                .email(EMAIL)
                .name("홍길동")
                .studentNum("183027")
                .affiliation("공과대학")
                .department("컴퓨터정보통신공학과")
                .carNum(carNumber)
                .isLight(true)
                .phoneNum("010-1111-2222")
                .isSaved(true)
                .eventId(EVENT_ID)
                .build();
    }
}
