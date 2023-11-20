package com.jnu.ticketcommon.exception;

public class InvalidPasswordException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new InvalidPasswordException();

    private InvalidPasswordException() {
        super(GlobalErrorCode.INVALID_PASSWORD);
    }
}
