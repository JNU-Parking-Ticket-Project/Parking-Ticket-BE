package com.jnu.ticketdomain.domains.council.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class SendFailEmailException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new SendFailEmailException();

    private SendFailEmailException() {
        super(CouncilErrorCode.FAILED_TO_SEND_EMAIL);
    }
}
