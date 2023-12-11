package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NoEventStockLeftException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NoEventStockLeftException();

    private NoEventStockLeftException() {
        super(EventErrorCode.NO_EVENT_STOCK_LEFT);
    }
}
