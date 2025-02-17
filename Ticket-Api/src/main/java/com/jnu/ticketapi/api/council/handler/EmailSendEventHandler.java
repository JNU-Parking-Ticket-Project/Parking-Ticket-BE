package com.jnu.ticketapi.api.council.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.MAX_EMAIL_SEND_RETRY;

import com.jnu.ticketdomain.domains.events.event.EventExpiredEvent;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.service.MailService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSendEventHandler {
    private final MailService mailService;
    private final RegistrationAdaptor registrationAdaptor;

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 30000))
    @SneakyThrows
    @Async
    @TransactionalEventListener(
            classes = EventExpiredEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventExpiredEvent eventExpiredEvent) {
        int startIndex = 0;
        int batchSize = 14;
        Page<Registration> registrations;
        Queue<Registration> failQueue = new LinkedList<>();
        Map<Registration, Integer> retryCounts = new HashMap<>(); // Map to track retry counts

        do {
            registrations =
                    registrationAdaptor.findByIsDeletedFalseAndIsSavedTrueByPage(
                            eventExpiredEvent.getEventId(), startIndex);

            List<Registration> batch = new ArrayList<>(batchSize);
            for (Registration registration : registrations.getContent()) {
                batch.add(registration);

                if (batch.size() == batchSize) {
                    processBatch(batch, failQueue, retryCounts);
                    batch.clear();

                    TimeUnit.SECONDS.sleep(1);
                }
            }

            if (!batch.isEmpty()) {
                processBatch(batch, failQueue, retryCounts);
                batch.clear();
                TimeUnit.SECONDS.sleep(1);
            }
            startIndex++;

        } while (registrations.hasNext());

        failOver(failQueue, retryCounts);
    }

    private void processBatch(
            List<Registration> batch,
            Queue<Registration> failQueue,
            Map<Registration, Integer> retryCounts) {
        for (Registration registration : batch) {
            try {
                boolean result =
                        mailService.sendRegistrationResultMail(
                                registration.getEmail(),
                                registration.getName(),
                                registration.getUser().getStatus().getValue(),
                                registration.getUser().getSequence());

                if (!result) {
                    failQueue.add(registration);
                    retryCounts.put(
                            registration,
                            retryCounts.getOrDefault(registration, 0) + 1); // Increment retry count
                }
            } catch (Exception e) {
                log.error(
                        "Email sending failed for {}: {}", registration.getEmail(), e.getMessage());
                failQueue.add(registration);
                retryCounts.put(
                        registration,
                        retryCounts.getOrDefault(registration, 0) + 1); // Increment retry count
            }
        }
    }

    private void failOver(Queue<Registration> failQueue, Map<Registration, Integer> retryCounts)
            throws InterruptedException {
        while (!failQueue.isEmpty()) {
            int queueSize = failQueue.size();
            for (int i = 0; i < queueSize; i++) {
                Registration registration = failQueue.poll();

                int retryCount = retryCounts.getOrDefault(registration, 0);
                if (retryCount >= MAX_EMAIL_SEND_RETRY) {
                    log.error("최대 10번 retry 실패시: {}", registration.getEmail());
                    continue;
                }
                try {
                    boolean result =
                            mailService.sendRegistrationResultMail(
                                    registration.getEmail(),
                                    registration.getName(),
                                    registration.getUser().getStatus().getValue(),
                                    registration.getUser().getSequence());

                    if (!result) {
                        retryCounts.put(registration, retryCount + 1);
                        failQueue.add(registration);
                        log.warn(
                                "Retry failed for email: {} (Attempt {})",
                                registration.getEmail(),
                                retryCount + 1);
                    }
                } catch (Exception e) {
                    log.error(
                            "Retry exception for email {}: {}",
                            registration.getEmail(),
                            e.getMessage());
                    retryCounts.put(registration, retryCount + 1);
                    failQueue.add(registration);
                }
            }
            TimeUnit.SECONDS.sleep(1);
        }
    }
}

