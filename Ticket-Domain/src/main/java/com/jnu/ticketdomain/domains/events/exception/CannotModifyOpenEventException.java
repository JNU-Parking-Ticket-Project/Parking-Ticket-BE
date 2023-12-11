package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class CannotModifyOpenEventException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new CannotModifyOpenEventException();

    private CannotModifyOpenEventException() {
        super(EventErrorCode.CANNOT_MODIFY_OPEN_EVENT);
    }
}
