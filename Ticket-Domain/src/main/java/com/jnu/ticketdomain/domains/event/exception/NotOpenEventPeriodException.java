package com.jnu.ticketdomain.domains.event.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotOpenEventPeriodException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new NotOpenEventPeriodException();

    private NotOpenEventPeriodException() {
        super(EventErrorCode.NOT_EVENT_OPENING_PERIOD);
    }
}
