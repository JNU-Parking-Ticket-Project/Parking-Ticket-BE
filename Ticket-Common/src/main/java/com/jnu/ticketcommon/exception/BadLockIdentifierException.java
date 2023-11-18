package com.jnu.ticketcommon.exception;

public class BadLockIdentifierException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new BadLockIdentifierException();

    private BadLockIdentifierException() {
        super(GlobalErrorCode.BAD_LOCK_IDENTIFIER);
    }
}
