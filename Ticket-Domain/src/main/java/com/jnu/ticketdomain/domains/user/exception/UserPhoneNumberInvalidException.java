package com.jnu.ticketdomain.domains.user.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class UserPhoneNumberInvalidException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new UserPhoneNumberInvalidException();

    private UserPhoneNumberInvalidException() {
        super(UserErrorCode.USER_PHONE_INVALID);
    }
}
