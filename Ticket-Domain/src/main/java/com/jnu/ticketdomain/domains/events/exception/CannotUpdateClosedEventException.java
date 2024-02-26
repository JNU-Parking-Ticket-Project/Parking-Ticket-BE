package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class CannotUpdateClosedEventException extends TicketCodeException {

        public static final TicketCodeException EXCEPTION = new CannotUpdateClosedEventException();

        private CannotUpdateClosedEventException() {
            super(EventErrorCode.CANNOT_UPDATE_CLOSED_EVENT);
        }
}
