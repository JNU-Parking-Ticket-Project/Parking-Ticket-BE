package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyDeletedStatusException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new AlreadyDeletedStatusException();

    private AlreadyDeletedStatusException() {
        super(EventErrorCode.ALREADY_DELETED_STATUS);
    }
}
