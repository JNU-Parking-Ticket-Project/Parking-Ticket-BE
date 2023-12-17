package com.jnu.ticketcommon.exception;

public class UnsupportedJwtException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new UnsupportedJwtException();

    private UnsupportedJwtException() {
        super(GlobalErrorCode.UNSUPPORTED_JWT);
    }
}
