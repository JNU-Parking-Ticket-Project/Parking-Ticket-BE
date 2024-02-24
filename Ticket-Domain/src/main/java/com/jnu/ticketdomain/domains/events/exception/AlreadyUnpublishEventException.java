package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyUnpublishEventException extends TicketCodeException {
    public static final AlreadyUnpublishEventException EXCEPTION = new AlreadyUnpublishEventException();

    private AlreadyUnpublishEventException() {
        super(EventErrorCode.ALREADY_UNPUBLISH_EVENT);
    }
}
