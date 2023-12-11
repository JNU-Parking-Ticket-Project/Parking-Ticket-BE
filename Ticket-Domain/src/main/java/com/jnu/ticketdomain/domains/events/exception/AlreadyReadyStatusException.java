package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyReadyStatusException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new AlreadyReadyStatusException();

    private AlreadyReadyStatusException() {
        super(EventErrorCode.ALREADY_PREPARING_STATUS);
    }
}
