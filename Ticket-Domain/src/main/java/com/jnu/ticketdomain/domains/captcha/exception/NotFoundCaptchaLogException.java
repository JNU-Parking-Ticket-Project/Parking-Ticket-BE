package com.jnu.ticketdomain.domains.captcha.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundCaptchaLogException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundCaptchaLogException();

    private NotFoundCaptchaLogException() {
        super(CaptchaErrorCode.NOT_FOUND_CAPTCHA_LOG);
    }
}
