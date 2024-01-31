package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class PublishStatusTrueException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new PublishStatusTrueException();

    private PublishStatusTrueException() {
        super(EventErrorCode.NOT_PUBLISH_FALSE_STATUS);
    }
}
