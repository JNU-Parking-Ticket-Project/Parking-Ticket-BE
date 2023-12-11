package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyCloseStatusException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new AlreadyCloseStatusException();

    private AlreadyCloseStatusException() {
        super(EventErrorCode.ALREADY_CLOSE_STATUS);
    }
}
