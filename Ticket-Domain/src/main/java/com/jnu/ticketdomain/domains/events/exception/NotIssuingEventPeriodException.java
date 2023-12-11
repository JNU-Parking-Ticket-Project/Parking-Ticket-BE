package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotIssuingEventPeriodException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotIssuingEventPeriodException();

    private NotIssuingEventPeriodException() {
        super(EventErrorCode.NOT_EVENT_ISSUING_PERIOD);
    }
}
