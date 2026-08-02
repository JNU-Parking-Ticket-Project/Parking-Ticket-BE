package com.jnu.ticketdomain.domains.email.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class FailedEmailOutboxNotFoundException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new FailedEmailOutboxNotFoundException();

    private FailedEmailOutboxNotFoundException() {
        super(EmailOutboxErrorCode.FAILED_OUTBOX_NOT_FOUND);
    }
}
