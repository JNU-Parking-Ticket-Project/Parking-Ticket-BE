package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotReadyEventStatusException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotReadyEventStatusException();

    private NotReadyEventStatusException() {
        super(EventErrorCode.NOT_EVENT_READY_STATUS);
    }
}
