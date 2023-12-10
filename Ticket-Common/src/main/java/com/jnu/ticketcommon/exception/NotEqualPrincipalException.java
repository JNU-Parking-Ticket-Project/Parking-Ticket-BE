package com.jnu.ticketcommon.exception;

public class NotEqualPrincipalException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotEqualPrincipalException();

    private NotEqualPrincipalException() {
        super(GlobalErrorCode.NOT_EQUAL_PRINCIPAL);
    }
}
