package com.jnu.ticketcommon.exception;

public class ExpiredTokenException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new ExpiredTokenException();

    private ExpiredTokenException() {
        super(GlobalErrorCode.TOKEN_EXPIRED);
    }
}
