package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class CannotUpdatePublishEventException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new CannotUpdatePublishEventException();

    private CannotUpdatePublishEventException() {
        super(EventErrorCode.CANNOT_UPDATE_PUBLISH_EVENT);
    }
}
