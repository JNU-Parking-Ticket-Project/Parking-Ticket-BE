package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class StillOpenEventException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new StillOpenEventException();

    private StillOpenEventException() {super(EventErrorCode.STILL_OPEN_EVENT);}
}
