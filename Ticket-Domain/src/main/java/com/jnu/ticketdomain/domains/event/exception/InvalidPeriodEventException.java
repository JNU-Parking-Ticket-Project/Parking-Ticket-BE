package com.jnu.ticketdomain.domains.event.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class InvalidPeriodEventException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new InvalidPeriodEventException();

    private InvalidPeriodEventException() {
        super(EventErrorCode.NOT_Event_ISSUING_PERIOD);
    }
}
