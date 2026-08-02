package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketapi.api.event.service.RegistrationResultPersistenceService;
import com.jnu.ticketapi.api.event.service.RegistrationResultPersistenceService.StreamDecisionAction;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.jnu.ticketinfrastructure.stream.RegistrationStreamMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventIssuedEventHandler implements RegistrationStreamMessageHandler {

    private static final Logger tracker = LoggerFactory.getLogger("processTracker");

    private final RegistrationResultPersistenceService registrationResultPersistenceService;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    private final ObjectMapper objectMapper;

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000))
    public void handle(StreamQueueMessage streamQueueMessage) {
        try {
            MDC.put("userId", String.valueOf(streamQueueMessage.getMessage().getUserId()));
            ChatMessage message = streamQueueMessage.getMessage();

            try {
                long savedAt = resolveScore(streamQueueMessage);
                if (message.hasJournalMetadata()) {
                    if (!message.hasJournalDecision()) {
                        throw new IllegalArgumentException(
                                "Incomplete registration admission journal message");
                    }
                    persistJournalDecision(message, savedAt);
                } else {
                    Registration registration =
                            objectMapper.readValue(message.getRegistration(), Registration.class);
                    persistLegacyRegistration(registration, message, savedAt);
                }
                acknowledge(streamQueueMessage);
            } catch (NoEventStockLeftException e) {
                tracker.info("해당 구간 잔여 여석이 없습니다.", e);
                acknowledge(streamQueueMessage);
            } catch (Exception e) {
                // ack 하지 않으면 Redis Stream pending entry로 남아 재처리할 수 있다.
                tracker.error("EventIssuedEventHandler Exception: ", e);
                throw new IllegalStateException(e);
            }
        } finally {
            MDC.clear();
        }
    }

    private void persistJournalDecision(ChatMessage message, long savedAt) {
        StreamDecisionAction action =
                registrationResultPersistenceService.recordStreamDecision(
                        message.getJournalId(),
                        message.getAdmissionEpoch(),
                        StockReservationResult.reserved(
                                message.getPosition(),
                                message.getResultStatus(),
                                message.getSequence(),
                                message.getRemainingAmount()),
                        System.currentTimeMillis());
        if (action == StreamDecisionAction.MATERIALIZE) {
            registrationResultPersistenceService.materializeConfirmedJournal(
                    message.getJournalId());
        } else if (action == StreamDecisionAction.DATABASE_FALLBACK) {
            registrationResultPersistenceService.persistJournalWithDatabaseFallback(
                    message.getJournalId(), savedAt);
        }
    }

    private void persistLegacyRegistration(
            Registration registration, ChatMessage message, long savedAt) {
        if (message.hasDecision()) {
            registrationResultPersistenceService.persistRedisReservation(
                    registration,
                    message.getUserId(),
                    message.getSectorId(),
                    message.getEventId(),
                    StockReservationResult.reserved(
                            message.getPosition(),
                            message.getResultStatus(),
                            message.getSequence(),
                            null),
                    savedAt);
            return;
        }
        registrationResultPersistenceService.persistWithDatabaseFallback(
                registration,
                message.getUserId(),
                message.getSectorId(),
                message.getEventId(),
                savedAt);
    }

    private long resolveScore(StreamQueueMessage streamQueueMessage) {
        String streamRecordId = streamQueueMessage.getRecordId();
        if (streamRecordId != null && streamRecordId.contains("-")) {
            return Long.parseLong(streamRecordId.substring(0, streamRecordId.indexOf('-')));
        }
        return System.currentTimeMillis();
    }

    private void acknowledge(StreamQueueMessage streamQueueMessage) {
        try {
            waitingQueueService.acknowledgeAndDelete(
                    resolveStreamKey(streamQueueMessage),
                    REDIS_EVENT_ISSUE_GROUP,
                    streamQueueMessage.getRecordId());
        } catch (Exception e) {
            tracker.error(
                    "Redis Stream ACK failed. recordId: {}", streamQueueMessage.getRecordId(), e);
        }
    }

    private String resolveStreamKey(StreamQueueMessage streamQueueMessage) {
        if (streamQueueMessage.getStreamKey() != null) {
            return streamQueueMessage.getStreamKey();
        }
        return waitingQueueService.eventStreamKey(streamQueueMessage.getMessage().getEventId());
    }
}
