package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyPublishEventException extends TicketCodeException {
    public static final AlreadyPublishEventException EXCEPTION = new AlreadyPublishEventException();

    private AlreadyPublishEventException() {
        super(EventErrorCode.ALREADY_PUBLISH_EVENT);
    }
}
