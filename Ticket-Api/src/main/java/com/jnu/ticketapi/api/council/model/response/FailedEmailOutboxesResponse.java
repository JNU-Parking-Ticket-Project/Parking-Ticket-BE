package com.jnu.ticketapi.api.council.model.response;


import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import java.util.List;
import org.springframework.data.domain.Page;

public record FailedEmailOutboxesResponse(
        List<FailedEmailOutboxResponse> outboxes,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static FailedEmailOutboxesResponse from(Page<EmailOutbox> outboxes) {
        return new FailedEmailOutboxesResponse(
                outboxes.getContent().stream().map(FailedEmailOutboxResponse::from).toList(),
                outboxes.getNumber(),
                outboxes.getSize(),
                outboxes.getTotalElements(),
                outboxes.getTotalPages());
    }
}
