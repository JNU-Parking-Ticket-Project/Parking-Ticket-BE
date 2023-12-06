package com.jnu.ticketdomain.domains.user.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class CredentialCodeNotExistException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new CredentialCodeNotExistException();

    private CredentialCodeNotExistException() {
        super(UserErrorCode.CREDENTIAL_CODE_NOT_EXIST_ERROR);
    }
}
