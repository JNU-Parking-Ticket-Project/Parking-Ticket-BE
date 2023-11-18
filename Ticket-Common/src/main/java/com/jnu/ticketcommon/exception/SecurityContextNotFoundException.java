package com.jnu.ticketcommon.exception;

public class SecurityContextNotFoundException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new SecurityContextNotFoundException();

    private SecurityContextNotFoundException() {
        super(GlobalErrorCode.SECURITY_CONTEXT_NOT_FOUND);
    }
}
