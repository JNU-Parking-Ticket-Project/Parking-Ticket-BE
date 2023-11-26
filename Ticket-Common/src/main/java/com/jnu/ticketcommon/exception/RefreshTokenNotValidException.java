package com.jnu.ticketcommon.exception;

public class RefreshTokenNotValidException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new RefreshTokenNotValidException();

    private RefreshTokenNotValidException() {
        super(GlobalErrorCode.REFRESH_TOKEN_NOT_VALID);
    }
}
