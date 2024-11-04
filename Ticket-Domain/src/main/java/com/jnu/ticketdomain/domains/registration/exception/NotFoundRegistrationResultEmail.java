package com.jnu.ticketdomain.domains.registration.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundRegistrationResultEmail extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundRegistrationResultEmail();

    private NotFoundRegistrationResultEmail() {
        super(RegistrationErrorCode.NOT_FOUND_REGISTRATION_RESULT_EMAIL_OUTBOX);
    }
}
