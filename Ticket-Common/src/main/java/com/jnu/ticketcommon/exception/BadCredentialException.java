package com.jnu.ticketcommon.exception;

public class BadCredentialException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new BadCredentialException();

    private BadCredentialException() {
        super(GlobalErrorCode.BAD_CREDENTIAL);
    }
}
