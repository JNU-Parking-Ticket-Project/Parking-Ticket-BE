package com.jnu.ticketcommon.exception;

public class AccessTokenExpiredException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AccessTokenExpiredException();

    private AccessTokenExpiredException() {
        super(GlobalErrorCode.ACCESS_TOKEN_EXPIRED);
    }
}
