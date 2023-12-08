package com.jnu.ticketdomain.domains.council.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyExistEmailException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AlreadyExistEmailException();

    private AlreadyExistEmailException() {
        super(CouncilErrorCode.ALREADY_EXIST_EMAIL);
    }
}
