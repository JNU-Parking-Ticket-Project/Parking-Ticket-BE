package com.jnu.ticketapi.api.event.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.jnu.ticketinfrastructure.stream.RegistrationStreamMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
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
    private final EmailOutboxAdaptor emailOutboxAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    private final SectorAdaptor sectorAdaptor;
    private final ObjectMapper objectMapper;

    @Value("${ticket.redis.stream.max-processing-failures:3}")
    private int maxProcessingFailures = 3;

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(StreamQueueMessage streamQueueMessage) {
        try {
            MDC.put("userId", String.valueOf(streamQueueMessage.getMessage().getUserId()));
            ChatMessage message = streamQueueMessage.getMessage();

            Sector sector = findSector(message);

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

                processQueueData(sector, registration, message, resolveScore(streamQueueMessage));
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

    @Recover
    public void recover(Exception exception, StreamQueueMessage streamQueueMessage) {
        if (streamQueueMessage.getRecordId() == null || waitingQueueService == null) {
            tracker.error(
                    "Redis Stream message exhausted retries without a recoverable record id",
                    exception);
            return;
        }

        try {
            DeadLetterTransferResult result =
                    waitingQueueService.recordProcessingFailure(
                            resolveStreamKey(streamQueueMessage),
                            REDIS_EVENT_ISSUE_GROUP,
                            streamQueueMessage.getRecordId(),
                            streamQueueMessage.getMessage(),
                            maxProcessingFailures,
                            exception);
            if (result.isMoved()) {
                tracker.error(
                        "Redis Stream message moved to DLQ after {} failed deliveries. recordId: {}",
                        result.getFailureCount(),
                        streamQueueMessage.getRecordId(),
                        exception);
                return;
            }
            tracker.warn(
                    "Redis Stream message remains pending after failed delivery {}/{}. recordId: {}",
                    result.getFailureCount(),
                    maxProcessingFailures,
                    streamQueueMessage.getRecordId());
        } catch (Exception recoveryException) {
            tracker.error(
                    "Failed to record Redis Stream processing failure. recordId: {}",
                    streamQueueMessage.getRecordId(),
                    recoveryException);
        }
    }

    /** 대기열에서 pop한 registration을 저장하는 시점에 순번과 결과를 확정하고 메일 outbox를 생성한다. */
    public void processQueueData(Sector sector, Registration registration, Long userId) {
        processQueueData(sector, registration, userId, (double) System.currentTimeMillis());
    }

    public void processQueueData(
            Sector sector, Registration registration, Long userId, Double score) {
        processQueueData(
                sector,
                registration,
                new ChatMessage(null, userId, sector.getId(), registration.getEventId()),
                score);
    }

    public void processQueueData(Sector sector, Registration registration, ChatMessage message) {
        processQueueData(sector, registration, message, (double) System.currentTimeMillis());
    }

    public void processQueueData(
            Sector sector, Registration registration, ChatMessage message, Double score) {
        User user = userAdaptor.findById(message.getUserId());
        saveRegistration(sector, user, registration, message, score);
    }

    private Sector findSector(ChatMessage message) {
        if (message.hasDecision()) {
            return sectorAdaptor.findById(message.getSectorId());
        }
        return sectorAdaptor.findByIdForUpdate(message.getSectorId());
    }

    private void saveRegistration(
            Sector sector,
            User user,
            Registration registration,
            ChatMessage message,
            Double score) {
        RegistrationDecision decision = resolveDecision(sector, message);
        int position = decision.position;

        if (!message.hasDecision() && !decision.isFail()) {
            sector.decreaseEventStock();
        }

        reflectUserState(user, decision);

        if (!registration.isSaved()) {
            // if문 사용 안됨.
            registration.finalSave(position, decision.resultStatus, decision.sequence);
            registration.setSector(sector);
            registration.setUser(user);
            registration.setSavedAt(score.longValue());
            Registration savedRegistration = registrationAdaptor.save(registration);
            emailOutboxAdaptor.saveRegistrationResultIfAbsent(savedRegistration);
        } else {
            registration.finalSave(position, decision.resultStatus, decision.sequence);
            registration.setSector(sector);
            registration.setUser(user);
            registration.setSavedAt(score.longValue());
            Registration savedRegistration = registrationAdaptor.saveAndFlush(registration);
            emailOutboxAdaptor.saveRegistrationResultIfAbsent(savedRegistration);
        }

        tracker.info(
                "Registration saved. position: {}, status: {}, sequence: {}",
                position,
                decision.resultStatus.getValue(),
                decision.sequence);
    }

    private RegistrationDecision resolveDecision(Sector sector, ChatMessage message) {
        if (message.hasDecision()) {
            return new RegistrationDecision(
                    message.getPosition(), message.getResultStatus(), message.getSequence());
        }
        int position =
                Math.toIntExact(registrationAdaptor.countSavedBySectorId(sector.getId())) + 1;
        return decideResult(sector, position);
    }

    private RegistrationDecision decideResult(Sector sector, int position) {
        if (position <= sector.getInitSectorCapacity()) {
            return new RegistrationDecision(position, UserStatus.SUCCESS, -2);
        }
        if (position <= sector.getIssueAmount()) {
            return new RegistrationDecision(
                    position, UserStatus.PREPARE, position - sector.getInitSectorCapacity());
        }
        return new RegistrationDecision(position, UserStatus.FAIL, -1);
    }

    private void reflectUserState(User user, RegistrationDecision decision) {
        if (decision.resultStatus == UserStatus.SUCCESS) {
            user.success();
            return;
        }
        if (decision.resultStatus == UserStatus.PREPARE) {
            user.prepare(decision.sequence);
            return;
        }
        user.fail();
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

    private static class RegistrationDecision {
        private final Integer position;
        private final UserStatus resultStatus;
        private final Integer sequence;

        private RegistrationDecision(Integer position, UserStatus resultStatus, Integer sequence) {
            this.position = position;
            this.resultStatus = resultStatus;
            this.sequence = sequence;
        }

        private boolean isFail() {
            return resultStatus == UserStatus.FAIL;
        }
    }
}
