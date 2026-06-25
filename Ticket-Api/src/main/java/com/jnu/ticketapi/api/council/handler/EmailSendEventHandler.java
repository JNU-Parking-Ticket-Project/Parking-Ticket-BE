package com.jnu.ticketapi.api.council.handler;


import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.events.event.SendEmailEvent;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
    private final RegistrationAdaptor registrationAdaptor;
    private final EmailOutboxAdaptor emailOutboxAdaptor;

    @Async
    @TransactionalEventListener(
            classes = SendEmailEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(SendEmailEvent sendEmailEvent) {
        int startIndex = 0;
        Page<Registration> registrations;

        do {
            registrations =
                    registrationAdaptor.findByIsDeletedFalseAndIsSavedTrueByPage(
                            sendEmailEvent.getEventId(), startIndex);

            for (Registration registration : registrations.getContent()) {
                emailOutboxAdaptor.saveRegistrationResultIfAbsent(registration);
            }
            startIndex++;

        } while (registrations.hasNext());

        log.info(
                "Manual SendEmailEvent outbox backfill completed. eventId: {}",
                sendEmailEvent.getEventId());
    }
}
