package com.jnu.ticketcommon.exception;

public class RefreshTokenExpiredException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new RefreshTokenExpiredException();

    private RefreshTokenExpiredException() {
        super(GlobalErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}
