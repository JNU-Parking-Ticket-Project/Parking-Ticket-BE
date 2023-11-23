package com.jnu.ticketcommon.exception;

public class AccessTokenNotValidException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AccessTokenNotValidException();

    private AccessTokenNotValidException() {
        super(GlobalErrorCode.ACCESS_TOKEN_NOT_VALID);
    }
}
