package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class CannotPublishClosedEventException extends TicketCodeException {
    public static final CannotPublishClosedEventException EXCEPTION = new CannotPublishClosedEventException();

    private CannotPublishClosedEventException() {
        super(EventErrorCode.CANNOT_PUBLSIH_CLOSED_EVENT);
    }
}
