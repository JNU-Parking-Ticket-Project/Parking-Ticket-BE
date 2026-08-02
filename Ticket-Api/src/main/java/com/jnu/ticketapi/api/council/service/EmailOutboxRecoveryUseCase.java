package com.jnu.ticketapi.api.council.service;


import com.jnu.ticketapi.api.council.model.response.FailedEmailOutboxesResponse;
import com.jnu.ticketapi.api.council.model.response.RequeueEmailOutboxResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.email.exception.FailedEmailOutboxNotFoundException;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class EmailOutboxRecoveryUseCase {
    private static final int MAX_PAGE_SIZE = 100;

    private final EventAdaptor eventAdaptor;
    private final EmailOutboxAdaptor emailOutboxAdaptor;

    @Transactional(readOnly = true)
    public FailedEmailOutboxesResponse findFailed(Long eventId, Pageable pageable) {
        eventAdaptor.findById(eventId);
        Pageable boundedPageable =
                PageRequest.of(
                        pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        Page<EmailOutbox> failedOutboxes =
                emailOutboxAdaptor.findFailedByEventId(eventId, boundedPageable);
        return FailedEmailOutboxesResponse.from(failedOutboxes);
    }

    @Transactional
    public RequeueEmailOutboxResponse requeue(Long eventId, Long outboxId) {
        eventAdaptor.findById(eventId);
        if (!emailOutboxAdaptor.requeueFailed(eventId, outboxId)) {
            throw FailedEmailOutboxNotFoundException.EXCEPTION;
        }
        return RequeueEmailOutboxResponse.pending(eventId, outboxId);
    }
}
