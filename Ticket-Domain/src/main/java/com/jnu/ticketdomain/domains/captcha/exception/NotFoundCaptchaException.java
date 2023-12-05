package com.jnu.ticketdomain.domains.captcha.exception;

import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundCaptchaException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundCaptchaException();

    private NotFoundCaptchaException() {
        super(CaptchaErrorCode.NOT_FOUND_CAPTCHA);
    }
}
