package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.api.event.service.RegistrationResultPersistenceService;
import com.jnu.ticketapi.api.event.service.RegistrationResultPersistenceService.StreamDecisionAction;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventIssuedEventHandlerTest {

    private static final String EVENT_STREAM_KEY = "쿠폰 발급 스트림:{3}";

    @Mock private RegistrationResultPersistenceService registrationResultPersistenceService;
    @Mock private WaitingQueueService waitingQueueService;

    private EventIssuedEventHandler eventIssuedEventHandler;

    @BeforeEach
    void setUp() {
        eventIssuedEventHandler =
                new EventIssuedEventHandler(
                        registrationResultPersistenceService, new ObjectMapper());
        ReflectionTestUtils.setField(
                eventIssuedEventHandler, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("DB 확정 서비스가 반환된 뒤 Stream record를 ACK하고 삭제한다")
    void handleAcknowledgesAfterDatabasePersistenceReturns() {
        StreamQueueMessage event =
                event(
                        "1234-0",
                        new ChatMessage(registrationJson(), 1L, 2L, 3L, 1, UserStatus.SUCCESS, -2));

        eventIssuedEventHandler.handle(event);

        ArgumentCaptor<StockReservationResult> reservationCaptor =
                ArgumentCaptor.forClass(StockReservationResult.class);
        verify(registrationResultPersistenceService)
                .persistRedisReservation(
                        any(Registration.class),
                        eq(1L),
                        eq(2L),
                        eq(3L),
                        reservationCaptor.capture(),
                        eq(1234L));
        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "1234-0");
    }

    @Test
    @DisplayName("결정 정보가 없는 레거시 메시지는 DB fallback으로 확정한다")
    void handleLegacyMessageWithDatabaseFallback() {
        StreamQueueMessage event = event("2-0", new ChatMessage(registrationJson(), 1L, 2L, 3L));

        eventIssuedEventHandler.handle(event);

        verify(registrationResultPersistenceService)
                .persistWithDatabaseFallback(
                        any(Registration.class), eq(1L), eq(2L), eq(3L), eq(2L));
        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "2-0");
    }

    @Test
    @DisplayName("DB 저장이 실패하면 Stream record를 ACK하지 않는다")
    void handleKeepsStreamRecordPendingOnPersistenceFailure() {
        StreamQueueMessage event =
                event(
                        "3-0",
                        new ChatMessage(registrationJson(), 1L, 2L, 3L, 1, UserStatus.SUCCESS, -2));
        when(registrationResultPersistenceService.persistRedisReservation(
                        any(), any(), any(), any(), any(), eq(3L)))
                .thenThrow(new IllegalStateException("DB 저장 실패"));

        assertThatThrownBy(() -> eventIssuedEventHandler.handle(event))
                .isInstanceOf(IllegalStateException.class);

        verify(waitingQueueService, never())
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "3-0");
    }

    @Test
    @DisplayName("확정된 저널 메시지는 Stream payload를 파싱하지 않고 본 저장한 뒤 ACK한다")
    void handleMaterializesConfirmedJournalWithoutParsingStreamPayload() {
        StreamQueueMessage event =
                event(
                        "35-0",
                        new ChatMessage(
                                "not-json", 1L, 2L, 3L, 1, UserStatus.SUCCESS, -2, 2, 40L, 7L));
        when(registrationResultPersistenceService.recordStreamDecision(
                        eq(40L), eq(7L), any(StockReservationResult.class), anyLong()))
                .thenReturn(StreamDecisionAction.MATERIALIZE);

        eventIssuedEventHandler.handle(event);

        ArgumentCaptor<StockReservationResult> reservationCaptor =
                ArgumentCaptor.forClass(StockReservationResult.class);
        verify(registrationResultPersistenceService)
                .recordStreamDecision(eq(40L), eq(7L), reservationCaptor.capture(), anyLong());
        assertThat(reservationCaptor.getValue().getPosition()).isEqualTo(1);
        assertThat(reservationCaptor.getValue().getRemainingAmount()).isEqualTo(2);
        verify(registrationResultPersistenceService).materializeConfirmedJournal(40L);
        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "35-0");
    }

    @Test
    @DisplayName("RECEIVED 저널을 먼저 본 consumer도 본 저장을 완료한 뒤 Stream record를 ACK한다")
    void handleMaterializesReceivedJournalBeforeAcknowledging() {
        StreamQueueMessage event = journalEvent("36-0");
        when(registrationResultPersistenceService.recordStreamDecision(
                        eq(40L), eq(7L), any(StockReservationResult.class), anyLong()))
                .thenReturn(StreamDecisionAction.MATERIALIZE);

        eventIssuedEventHandler.handle(event);

        verify(registrationResultPersistenceService).materializeConfirmedJournal(40L);
        verify(registrationResultPersistenceService, never())
                .persistJournalWithDatabaseFallback(any(), any(Long.class));
        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "36-0");
    }

    @Test
    @DisplayName("RECEIVED 저널의 본 저장이 실패하면 Stream record를 ACK하지 않는다")
    void handleKeepsReceivedJournalPendingOnMaterializationFailure() {
        StreamQueueMessage event = journalEvent("36-1");
        when(registrationResultPersistenceService.recordStreamDecision(
                        eq(40L), eq(7L), any(StockReservationResult.class), anyLong()))
                .thenReturn(StreamDecisionAction.MATERIALIZE);
        when(registrationResultPersistenceService.materializeConfirmedJournal(40L))
                .thenThrow(new IllegalStateException("신청서 본 저장 실패"));

        assertThatThrownBy(() -> eventIssuedEventHandler.handle(event))
                .isInstanceOf(IllegalStateException.class);

        verify(waitingQueueService, never())
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "36-1");
    }

    @Test
    @DisplayName("fallback 전환 뒤의 저널 메시지는 DB fallback으로 본 저장한 뒤 ACK한다")
    void handlePersistsJournalWithDatabaseFallbackAndAcknowledges() {
        StreamQueueMessage event = journalEvent("37-0");
        when(registrationResultPersistenceService.recordStreamDecision(
                        eq(40L), eq(7L), any(StockReservationResult.class), anyLong()))
                .thenReturn(StreamDecisionAction.DATABASE_FALLBACK);

        eventIssuedEventHandler.handle(event);

        verify(registrationResultPersistenceService).persistJournalWithDatabaseFallback(40L, 37L);
        verify(registrationResultPersistenceService, never()).materializeConfirmedJournal(any());
        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "37-0");
    }

    @Test
    @DisplayName("이미 처리된 저널 메시지는 본 저장 없이 Stream record만 ACK한다")
    void handleAcknowledgesJournalWithoutAdditionalPersistence() {
        StreamQueueMessage event = journalEvent("38-0");
        when(registrationResultPersistenceService.recordStreamDecision(
                        eq(40L), eq(7L), any(StockReservationResult.class), anyLong()))
                .thenReturn(StreamDecisionAction.ACK_ONLY);

        eventIssuedEventHandler.handle(event);

        verify(registrationResultPersistenceService, never()).materializeConfirmedJournal(any());
        verify(registrationResultPersistenceService, never())
                .persistJournalWithDatabaseFallback(any(), any(Long.class));
        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "38-0");
    }

    @Test
    @DisplayName("잘못된 payload는 Stream record를 ACK하지 않는다")
    void handleKeepsInvalidPayloadPending() {
        StreamQueueMessage event = event("4-0", new ChatMessage("not-json", 1L, 2L, 3L));

        assertThatThrownBy(() -> eventIssuedEventHandler.handle(event))
                .isInstanceOf(IllegalStateException.class);

        verify(registrationResultPersistenceService, never())
                .persistWithDatabaseFallback(any(), any(), any(), any(), any(Long.class));
        verify(waitingQueueService, never())
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "4-0");
    }

    @Test
    @DisplayName("레거시 메시지 처리 시 DB 재고가 없으면 ACK하고 종료한다")
    void handleAcknowledgesLegacyMessageWhenDatabaseStockIsEmpty() {
        StreamQueueMessage event = event("5-0", new ChatMessage(registrationJson(), 1L, 2L, 3L));
        when(registrationResultPersistenceService.persistWithDatabaseFallback(
                        any(), eq(1L), eq(2L), eq(3L), eq(5L)))
                .thenThrow(NoEventStockLeftException.EXCEPTION);

        eventIssuedEventHandler.handle(event);

        verify(waitingQueueService)
                .acknowledgeAndDelete(EVENT_STREAM_KEY, REDIS_EVENT_ISSUE_GROUP, "5-0");
    }

    private StreamQueueMessage event(String recordId, ChatMessage message) {
        return new StreamQueueMessage(EVENT_STREAM_KEY, recordId, message);
    }

    private StreamQueueMessage journalEvent(String recordId) {
        return event(
                recordId,
                new ChatMessage("not-json", 1L, 2L, 3L, 1, UserStatus.SUCCESS, -2, 2, 40L, 7L));
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
