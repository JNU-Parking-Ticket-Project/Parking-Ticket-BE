package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyOpenStatusException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new AlreadyOpenStatusException();

    private AlreadyOpenStatusException() {
        super(EventErrorCode.ALREADY_OPEN_STATUS);
    }
}
