package com.jnu.ticketdomain.domains.registration.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class AlreadyExistRegistrationException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AlreadyExistRegistrationException();

    private AlreadyExistRegistrationException() {
        super(RegistrationErrorCode.ALREADY_EXIST_REGISTRATION);
    }
}
