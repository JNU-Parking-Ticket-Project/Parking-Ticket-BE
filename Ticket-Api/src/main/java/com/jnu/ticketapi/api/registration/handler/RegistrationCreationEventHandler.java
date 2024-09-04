package com.jnu.ticketapi.api.registration.handler;


import com.jnu.ticketdomain.domains.registration.event.RegistrationCreationEvent;
import com.jnu.ticketinfrastructure.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class RegistrationCreationEventHandler {
    private final MailService mailService;

    @Async
    @TransactionalEventListener(
            classes = RegistrationCreationEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Deprecated
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000))
    public void handle(RegistrationCreationEvent event) {
        // 새로운 persistence context에서 User를 조회 (User를 영속화 하기 위해)
        //        mailService.sendRegistrationResultMail(
        //                event.getEmail(), event.getName(), event.getStatus(),
        // event.getSequence());
    }
}
