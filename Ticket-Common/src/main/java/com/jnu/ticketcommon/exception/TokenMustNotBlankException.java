package com.jnu.ticketcommon.exception;

public class TokenMustNotBlankException extends TicketCodeException{
    public static final TicketCodeException EXCEPTION = new TokenMustNotBlankException();

    private TokenMustNotBlankException(){
        super(GlobalErrorCode.TOKEN_MUST_NOT_BLANK);
    }
}
