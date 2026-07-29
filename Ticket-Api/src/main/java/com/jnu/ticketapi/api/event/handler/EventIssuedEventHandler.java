package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventIssuedEventHandler implements RegistrationStreamMessageHandler {

    private static final Logger tracker = LoggerFactory.getLogger("processTracker");

    private final RegistrationAdaptor registrationAdaptor;
    private final UserAdaptor userAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    private final SectorAdaptor sectorAdaptor;
    private final ObjectMapper objectMapper;

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(StreamQueueMessage streamQueueMessage) {
        try {
            MDC.put("userId", String.valueOf(streamQueueMessage.getMessage().getUserId()));

            Sector sector = sectorAdaptor.findById(streamQueueMessage.getMessage().getSectorId());

            try {
                Registration registration =
                        objectMapper.readValue(
                                streamQueueMessage.getMessage().getRegistration(),
                                Registration.class);

                if (isAlreadySaved(registration)) {
                    tracker.info("Already saved, ignored");
                    acknowledgeAfterCommit(streamQueueMessage);
                    return;
                }
                tracker.info(
                        "현재구간 정보, sectorId: {}, 정원여석: {}, 예비여석: {}, 총 여석: {},",
                        sector.getId(),
                        sector.getSectorCapacity(),
                        sector.getReserve(),
                        sector.getRemainingAmount());

                processQueueData(
                        sector,
                        registration,
                        streamQueueMessage.getMessage().getUserId(),
                        resolveScore(streamQueueMessage));
                acknowledgeAfterCommit(streamQueueMessage);

                // sectorAdaptor.save(sector); 데드락 문제 임시 해결
            } catch (NoEventStockLeftException e) {
                tracker.info("해당 구간 잔여 여석이 없습니다.", e);
                acknowledgeAfterCommit(streamQueueMessage);
            } catch (Exception e) {
                // ack 하지 않으면 Redis Stream pending entry로 남아 재처리할 수 있다.
                tracker.error("EventIssuedEventHandler Exception: ", e);
                throw new IllegalStateException(e);
            }
        } finally {
            MDC.clear();
        }
    }

    /** 대기열에서 pop한 registration을 저장하고 savedAt 기준 순서를 보존한다. */
    public void processQueueData(
            Sector sector, Registration registration, Long userId, Double score) {
        User user = userAdaptor.findById(userId);
        saveRegistration(sector, user, registration, score);
    }

    private void saveRegistration(
            Sector sector, User user, Registration registration, Double score) {
        if (!sector.isRemainingAmount()) {
            tracker.info("[No seats remaining]. Registration: {}", registration);
            throw NoEventStockLeftException.EXCEPTION;
        }

        if (!registration.isSaved()) {
            // if문 사용 안됨.
            registration.finalSave();
            registration.setSector(sector);
            registration.setUser(user);
            registration.setSavedAt(score.longValue());
            registrationAdaptor.save(registration);
            return;
        }

        registration.setSector(sector);
        registration.setUser(user);
        registration.setSavedAt(score.longValue());
        registrationAdaptor.saveAndFlush(registration);

        tracker.info("Registration saved");
    }

    private Double resolveScore(StreamQueueMessage streamQueueMessage) {
        String streamRecordId = streamQueueMessage.getRecordId();
        if (streamRecordId != null && streamRecordId.contains("-")) {
            return Double.valueOf(streamRecordId.substring(0, streamRecordId.indexOf('-')));
        }
        return (double) System.currentTimeMillis();
    }

    private boolean isAlreadySaved(Registration registration) {
        if (registration.getId() != null
                && Boolean.TRUE.equals(
                        registrationAdaptor.existsByIdAndIsSavedTrue(registration.getId()))) {
            return true;
        }
        return registration.getEventId() != null
                && Boolean.TRUE.equals(
                        registrationAdaptor.existsByEmailAndIsSavedTrue(
                                registration.getEmail(), registration.getEventId()));
    }

    private void acknowledgeAfterCommit(StreamQueueMessage streamQueueMessage) {
        if (streamQueueMessage.getRecordId() == null) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            acknowledge(streamQueueMessage);
                        }
                    });
            return;
        }

        acknowledge(streamQueueMessage);
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
