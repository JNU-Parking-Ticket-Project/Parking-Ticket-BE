package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class InvalidPeriodEventException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new InvalidPeriodEventException();

    private InvalidPeriodEventException() {
        super(EventErrorCode.NOT_EVENT_ISSUING_PERIOD);
    }
}
