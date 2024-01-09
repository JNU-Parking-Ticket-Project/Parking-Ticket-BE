package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotOpenEventStatusException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotOpenEventStatusException();

    private NotOpenEventStatusException() {
        super(EventErrorCode.NOT_EVENT_OPEN_STATUS);
    }
}
