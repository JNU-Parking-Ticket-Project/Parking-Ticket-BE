package com.jnu.ticketdomain.domains.events.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class RedisStockUnavailableException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new RedisStockUnavailableException();

    private RedisStockUnavailableException() {
        super(EventErrorCode.REDIS_STOCK_UNAVAILABLE);
    }
}
