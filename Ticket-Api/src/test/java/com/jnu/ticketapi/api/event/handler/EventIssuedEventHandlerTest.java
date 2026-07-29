package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class EventIssuedEventHandlerTest {

    private static final String EVENT_STREAM_KEY = "쿠폰 발급 스트림:{3}";

    @Mock private RegistrationAdaptor registrationAdaptor;
    @Mock private UserAdaptor userAdaptor;
    @Mock private EmailOutboxAdaptor emailOutboxAdaptor;
    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private Sector sector;
    @Mock private User queuedUser;

    private EventIssuedEventHandler eventIssuedEventHandler;

    @BeforeEach
    void setUp() {
        eventIssuedEventHandler =
                new EventIssuedEventHandler(
                        registrationAdaptor,
                        userAdaptor,
                        emailOutboxAdaptor,
                        sectorAdaptor,
                        new ObjectMapper());
        ReflectionTestUtils.setField(
                eventIssuedEventHandler, "waitingQueueService", waitingQueueService);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("DB 트랜잭션이 커밋된 뒤에만 Stream record를 ACK하고 삭제한다")
    void handleAcknowledgesStreamRecordAfterCommit() {
        EventIssuedEvent event = event("1-0", registrationJson());
        givenQueuedRegistrationSector();
        when(userAdaptor.findById(1L)).thenReturn(queuedUser);
        when(registrationAdaptor.save(any(Registration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        beginTransactionSynchronization();

        eventIssuedEventHandler.handle(event);

        verify(waitingQueueService, never())
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "1-0");

        commitSynchronizations();

        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "1-0");
    }

    @Test
    @DisplayName("처리에 실패하면 예외를 다시 던지고 Stream record를 ACK하지 않는다")
    void handleKeepsStreamRecordPendingOnFailure() {
        EventIssuedEvent event = event("2-0", "not-json");
        when(sectorAdaptor.findByIdForUpdate(2L)).thenReturn(sector);

        assertThatThrownBy(() -> eventIssuedEventHandler.handle(event))
                .isInstanceOf(IllegalStateException.class);

        verify(waitingQueueService, never())
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "2-0");
    }

    @Test
    @DisplayName("로컬 재시도가 모두 실패하면 delivery 실패 횟수를 기록한다")
    void recoverRecordsExhaustedDeliveryFailure() {
        EventIssuedEvent event = event("2-0", "not-json");
        IllegalStateException failure = new IllegalStateException("DB 저장 실패");
        when(waitingQueueService.recordProcessingFailure(
                        EVENT_STREAM_KEY,
                        REDIS_EVENT_ISSUE_GROUP,
                        "2-0",
                        event.getMessage(),
                        3,
                        failure))
                .thenReturn(new DeadLetterTransferResult(1, false));

        eventIssuedEventHandler.recover(failure, event);

        verify(waitingQueueService)
                .recordProcessingFailure(
                        EVENT_STREAM_KEY,
                        REDIS_EVENT_ISSUE_GROUP,
                        "2-0",
                        event.getMessage(),
                        3,
                        failure);
        verify(waitingQueueService, never())
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "2-0");
    }

    @Test
    @DisplayName("Stream record id가 없는 이전 이벤트는 DLQ 실패 횟수를 기록하지 않는다")
    void recoverSkipsLegacyEventWithoutStreamRecordId() {
        EventIssuedEvent event = EventIssuedEvent.from(new ChatMessage("{}", 1L, 2L, 3L), 1_000D);

        eventIssuedEventHandler.recover(new IllegalStateException("저장 실패"), event);

        verify(waitingQueueService, never())
                .recordProcessingFailure(
                        any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    @DisplayName("ID가 없는 신규 신청도 같은 이벤트와 이메일로 이미 저장됐다면 재처리하지 않는다")
    void handleSkipsRedeliveredRegistrationWithoutId() {
        EventIssuedEvent event = event("3-0", registrationJson());
        when(sectorAdaptor.findByIdForUpdate(2L)).thenReturn(sector);
        when(registrationAdaptor.existsByEmailAndIsSavedTrue("student@jnu.ac.kr", 3L))
                .thenReturn(true);

        eventIssuedEventHandler.handle(event);

        verify(userAdaptor, never()).findById(1L);
        verify(registrationAdaptor, never()).save(any());
        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "3-0");
    }

    @Test
    @DisplayName("정원 이내 position이면 합격으로 확정하고 outbox를 생성한다")
    void processQueueDataDecidesSuccess() {
        Registration registration = registration();
        User user = user();
        givenSector(2, 4);
        when(registrationAdaptor.countSavedBySectorId(1L)).thenReturn(0L);
        when(userAdaptor.findById(100L)).thenReturn(user);
        when(registrationAdaptor.save(registration)).thenReturn(registration);

        eventIssuedEventHandler.processQueueData(sector, registration, 100L, 1_234D);

        assertThat(registration.getPosition()).isEqualTo(1);
        assertThat(registration.getResultStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(registration.getSequence()).isEqualTo(-2);
        assertThat(registration.getSavedAt()).isEqualTo(1_234L);
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(user.getSequence()).isEqualTo(-2);
        verify(sector).decreaseEventStock();
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    @Test
    @DisplayName("정원 초과 예비 정원 이내 position이면 예비 번호로 확정한다")
    void processQueueDataDecidesPrepare() {
        Registration registration = registration();
        User user = user();
        givenSector(2, 4);
        when(registrationAdaptor.countSavedBySectorId(1L)).thenReturn(2L);
        when(userAdaptor.findById(100L)).thenReturn(user);
        when(registrationAdaptor.save(registration)).thenReturn(registration);

        eventIssuedEventHandler.processQueueData(sector, registration, 100L);

        assertThat(registration.getPosition()).isEqualTo(3);
        assertThat(registration.getResultStatus()).isEqualTo(UserStatus.PREPARE);
        assertThat(registration.getSequence()).isEqualTo(1);
        assertThat(user.getStatus()).isEqualTo(UserStatus.PREPARE);
        assertThat(user.getSequence()).isEqualTo(1);
        verify(sector).decreaseEventStock();
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    @Test
    @DisplayName("예비 정원까지 초과한 position이면 불합격으로 저장하고 재고는 차감하지 않는다")
    void processQueueDataDecidesFailWithoutDecreasingStock() {
        Registration registration = registration();
        User user = user();
        givenSector(2, 4);
        when(registrationAdaptor.countSavedBySectorId(1L)).thenReturn(4L);
        when(userAdaptor.findById(100L)).thenReturn(user);
        when(registrationAdaptor.save(registration)).thenReturn(registration);

        eventIssuedEventHandler.processQueueData(sector, registration, 100L);

        assertThat(registration.getPosition()).isEqualTo(5);
        assertThat(registration.getResultStatus()).isEqualTo(UserStatus.FAIL);
        assertThat(registration.getSequence()).isEqualTo(-1);
        assertThat(user.getStatus()).isEqualTo(UserStatus.FAIL);
        assertThat(user.getSequence()).isEqualTo(-1);
        verify(sector, never()).decreaseEventStock();
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    @Test
    @DisplayName("Redis에서 확정된 position과 결과가 있으면 DB count 없이 그대로 저장한다")
    void processQueueDataUsesRedisDecisionWithoutCountingOrDecreasingStock() {
        Registration registration = registration();
        User user = user();
        ChatMessage message = new ChatMessage("{}", 100L, 1L, 10L, 3, UserStatus.PREPARE, 1);
        when(userAdaptor.findById(100L)).thenReturn(user);
        when(registrationAdaptor.save(registration)).thenReturn(registration);

        eventIssuedEventHandler.processQueueData(sector, registration, message);

        assertThat(registration.getPosition()).isEqualTo(3);
        assertThat(registration.getResultStatus()).isEqualTo(UserStatus.PREPARE);
        assertThat(registration.getSequence()).isEqualTo(1);
        assertThat(user.getStatus()).isEqualTo(UserStatus.PREPARE);
        assertThat(user.getSequence()).isEqualTo(1);
        verify(registrationAdaptor, never())
                .countSavedBySectorId(org.mockito.ArgumentMatchers.any());
        verify(sector, never()).decreaseEventStock();
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    private void givenQueuedRegistrationSector() {
        when(sectorAdaptor.findByIdForUpdate(2L)).thenReturn(sector);
        when(sector.getId()).thenReturn(2L);
        when(sector.getInitSectorCapacity()).thenReturn(1);
        when(registrationAdaptor.countSavedBySectorId(2L)).thenReturn(0L);
    }

    private void givenSector(int initSectorCapacity, int issueAmount) {
        when(sector.getId()).thenReturn(1L);
        when(sector.getInitSectorCapacity()).thenReturn(initSectorCapacity);
        org.mockito.Mockito.lenient().when(sector.getIssueAmount()).thenReturn(issueAmount);
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private void commitSynchronizations() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
    }

    private EventIssuedEvent event(String recordId, String registration) {
        return EventIssuedEvent.from(
                new ChatMessage(registration, 1L, 2L, 3L), recordId, EVENT_STREAM_KEY);
    }

    private String registrationJson() {
        return "{\"email\":\"student@jnu.ac.kr\","
                + "\"name\":\"학생\","
                + "\"studentNum\":\"20240001\","
                + "\"carNum\":\"12가3456\","
                + "\"phoneNum\":\"010-0000-0000\","
                + "\"isLight\":false,"
                + "\"isSaved\":false,"
                + "\"isDeleted\":false,"
                + "\"eventId\":3}";
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
                        .eventId(10L)
                        .build();
        registration.setId(10L);
        return registration;
    }

    private User user() {
        return User.builder()
                .email("student@jnu.ac.kr")
                .pwd("encoded-password")
                .userRole(UserRole.USER)
                .build();
    }
}
