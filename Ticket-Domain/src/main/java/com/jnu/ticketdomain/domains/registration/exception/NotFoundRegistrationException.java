package com.jnu.ticketdomain.domains.registration.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundRegistrationException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundRegistrationException();

    private NotFoundRegistrationException() {
        super(RegistrationErrorCode.NOT_FOUND_REGISTRATION);
    }
}
