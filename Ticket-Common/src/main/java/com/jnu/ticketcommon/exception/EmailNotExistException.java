package com.jnu.ticketcommon.exception;

public class EmailNotExistException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new EmailNotExistException();

    private EmailNotExistException() {
        super(GlobalErrorCode.EMAIL_NOT_EXIST);
    }
}
