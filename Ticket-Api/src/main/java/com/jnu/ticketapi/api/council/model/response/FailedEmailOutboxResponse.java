package com.jnu.ticketapi.api.council.model.response;


import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import java.time.LocalDateTime;

public record FailedEmailOutboxResponse(
        Long outboxId,
        Long eventId,
        Long registrationId,
        String email,
        String name,
        String resultStatus,
        Integer sequence,
        Integer retryCount,
        LocalDateTime failedAt,
        String lastError) {

    public static FailedEmailOutboxResponse from(EmailOutbox outbox) {
        return new FailedEmailOutboxResponse(
                outbox.getId(),
                outbox.getEventId(),
                outbox.getRegistrationId(),
                outbox.getEmail(),
                outbox.getName(),
                outbox.getResultStatus().getValue(),
                outbox.getSequence(),
                outbox.getRetryCount(),
                outbox.getFailedAt(),
                outbox.getLastError());
    }
}
