package com.jnu.ticketdomain.domains.captcha.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class WrongCaptchaCodeException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new WrongCaptchaCodeException();

    private WrongCaptchaCodeException() {
        super(CaptchaErrorCode.WRONG_CAPTCHA_CODE);
    }
}
