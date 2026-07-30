package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
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
    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private Sector sector;
    @Mock private User user;

    private EventIssuedEventHandler eventIssuedEventHandler;

    @BeforeEach
    void setUp() {
        eventIssuedEventHandler =
                new EventIssuedEventHandler(
                        registrationAdaptor, userAdaptor, sectorAdaptor, new ObjectMapper());
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
        StreamQueueMessage event = event("1-0", registrationJson());
        when(sectorAdaptor.findById(2L)).thenReturn(sector);
        when(sector.isRemainingAmount()).thenReturn(true);
        when(userAdaptor.findById(1L)).thenReturn(user);
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
        StreamQueueMessage event = event("2-0", "not-json");
        when(sectorAdaptor.findById(2L)).thenReturn(sector);

        assertThatThrownBy(() -> eventIssuedEventHandler.handle(event))
                .isInstanceOf(IllegalStateException.class);

        verify(waitingQueueService, never())
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "2-0");
    }

    @Test
    @DisplayName("ID가 없는 신규 신청도 같은 이벤트와 이메일로 이미 저장됐다면 재처리하지 않는다")
    void handleSkipsRedeliveredRegistrationWithoutId() {
        StreamQueueMessage event = event("3-0", registrationJson());
        when(sectorAdaptor.findById(2L)).thenReturn(sector);
        when(registrationAdaptor.existsByEmailAndIsSavedTrue("student@jnu.ac.kr", 3L))
                .thenReturn(true);

        eventIssuedEventHandler.handle(event);

        verify(userAdaptor, never()).findById(1L);
        verify(registrationAdaptor, never()).save(org.mockito.ArgumentMatchers.any());
        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "3-0");
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

    private StreamQueueMessage event(String recordId, String registration) {
        return new StreamQueueMessage(
                EVENT_STREAM_KEY, recordId, new ChatMessage(registration, 1L, 2L, 3L));
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
}
