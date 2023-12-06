package com.jnu.ticketdomain.domains.captcha.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundCaptchaPendingException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundCaptchaPendingException();

    private NotFoundCaptchaPendingException() {
        super(CaptchaErrorCode.NOT_FOUND_CAPTCHA_PENDING);
    }
}
