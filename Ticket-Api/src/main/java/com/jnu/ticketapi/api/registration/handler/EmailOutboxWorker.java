package com.jnu.ticketapi.api.registration.handler;


import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketinfrastructure.service.MailService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "mail.outbox.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxWorker {
    private static final int BATCH_SIZE = 20;
    private static final long STALE_PROCESSING_MINUTES = 10;

    private final EmailOutboxAdaptor emailOutboxAdaptor;
    private final MailService mailService;

    @Scheduled(
            fixedDelayString = "${mail.outbox.fixed-delay-ms:1000}",
            initialDelayString = "${mail.outbox.initial-delay-ms:1000}")
    public void sendPendingRegistrationResultMail() {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(STALE_PROCESSING_MINUTES);
        List<EmailOutbox> pendingOutboxes = emailOutboxAdaptor.findPending(BATCH_SIZE, staleBefore);

        for (EmailOutbox outbox : pendingOutboxes) {
            if (!emailOutboxAdaptor.claim(outbox.getId(), staleBefore)) {
                continue;
            }
            send(outbox);
        }
    }

    private void send(EmailOutbox outbox) {
        boolean sent =
                mailService.sendRegistrationResultMail(
                        outbox.getEmail(),
                        outbox.getName(),
                        outbox.getResultStatus().getValue(),
                        outbox.getSequence());

        if (sent) {
            emailOutboxAdaptor.markSent(outbox.getId());
            return;
        }

        emailOutboxAdaptor.releaseAfterFailure(outbox.getId());
        log.warn("신청 결과 메일 outbox 발송 실패. outboxId: {}", outbox.getId());
    }
}
