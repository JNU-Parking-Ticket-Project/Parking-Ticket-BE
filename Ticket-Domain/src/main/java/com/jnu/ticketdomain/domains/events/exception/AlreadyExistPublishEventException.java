package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyExistPublishEventException extends TicketCodeException {
    public static final AlreadyExistPublishEventException EXCEPTION = new AlreadyExistPublishEventException();

    private AlreadyExistPublishEventException() {
        super(EventErrorCode.ALREADY_EXIST_PUBLISH_EVENT);
    }
}
