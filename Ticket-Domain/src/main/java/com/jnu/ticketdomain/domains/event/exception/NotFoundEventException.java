package com.jnu.ticketdomain.domains.event.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundEventException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundEventException();

    private NotFoundEventException() {
        super(EventErrorCode.NOT_FOUND_EVENT);
    }
}
