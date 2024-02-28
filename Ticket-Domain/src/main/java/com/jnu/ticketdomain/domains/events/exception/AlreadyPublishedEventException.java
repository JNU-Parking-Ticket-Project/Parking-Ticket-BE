package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyPublishedEventException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AlreadyPublishedEventException();

    private AlreadyPublishedEventException() {
        super(EventErrorCode.ALREADY_PUBLISHED_EVENT);
    }
}
