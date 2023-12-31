package com.jnu.ticketcommon.exception;

public class AuthenticationNotValidException extends TicketCodeException{
    public static final TicketCodeException EXCEPTION = new AuthenticationNotValidException();

    private AuthenticationNotValidException(){
        super(GlobalErrorCode.AUTHENTICATION_NOT_VALID);
    }
}
