package com.jnu.ticketcommon.exception;

public class InvalidTokenException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new InvalidTokenException();

    private InvalidTokenException() {
        super(GlobalErrorCode.INVALID_TOKEN);
    }
}
