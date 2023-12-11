package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyCalculatingStatusException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new AlreadyCalculatingStatusException();

    private AlreadyCalculatingStatusException() {
        super(EventErrorCode.ALREADY_CALCULATING_STATUS);
    }
}
