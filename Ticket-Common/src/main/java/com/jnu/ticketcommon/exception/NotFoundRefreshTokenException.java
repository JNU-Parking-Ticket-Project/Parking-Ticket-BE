package com.jnu.ticketcommon.exception;

public class NotFoundRefreshTokenException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundRefreshTokenException();

    private NotFoundRefreshTokenException() {
        super(GlobalErrorCode.NOT_FOUND_REFRESH_TOKEN);
    }
}
