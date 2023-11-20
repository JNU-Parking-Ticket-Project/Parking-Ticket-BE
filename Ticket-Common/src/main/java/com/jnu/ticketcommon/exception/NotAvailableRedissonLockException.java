package com.jnu.ticketcommon.exception;

public class NotAvailableRedissonLockException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new NotAvailableRedissonLockException();

    private NotAvailableRedissonLockException() {
        super(GlobalErrorCode.NOT_AVAILABLE_REDISSON_LOCK);
    }
}
