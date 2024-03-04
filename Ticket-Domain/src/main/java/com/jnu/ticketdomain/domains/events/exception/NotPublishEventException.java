package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotPublishEventException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotPublishEventException();

    private NotPublishEventException() {
        super(EventErrorCode.NOT_PUBLISH_EVENT);
    }
}
