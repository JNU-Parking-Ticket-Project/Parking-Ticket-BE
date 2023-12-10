package com.jnu.ticketdomain.domains.user.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class EmptyPhoneNumException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new EmptyPhoneNumException();

    private EmptyPhoneNumException() {
        super(UserErrorCode.USER_PHONE_EMPTY);
    }
}
