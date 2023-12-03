package com.jnu.ticketdomain.domains.user.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundUserException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundUserException();

    private NotFoundUserException() {
        super(UserErrorCode.NOT_FOUND_USER);
    }
}
