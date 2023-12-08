package com.jnu.ticketdomain.domains.council.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class IsNotCouncilException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new IsNotCouncilException();

    private IsNotCouncilException() {
        super(CouncilErrorCode.IS_NOT_COUNCIL);
    }
}
