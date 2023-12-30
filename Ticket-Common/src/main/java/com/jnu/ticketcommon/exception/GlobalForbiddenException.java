package com.jnu.ticketcommon.exception;

public class GlobalForbiddenException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new GlobalForbiddenException();

    private GlobalForbiddenException() {
        super(GlobalErrorCode.GLOBAL_FORBIDDEN);
    }
}
