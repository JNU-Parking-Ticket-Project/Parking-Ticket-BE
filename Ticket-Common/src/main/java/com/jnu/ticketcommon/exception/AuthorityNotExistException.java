package com.jnu.ticketcommon.exception;

public class AuthorityNotExistException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AuthorityNotExistException();

    private AuthorityNotExistException() {
        super(GlobalErrorCode.AUTHORITY_NOT_EXIST);
    }
}
