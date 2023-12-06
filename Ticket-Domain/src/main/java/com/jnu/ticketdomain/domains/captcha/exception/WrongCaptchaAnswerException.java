package com.jnu.ticketdomain.domains.captcha.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class WrongCaptchaAnswerException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new WrongCaptchaAnswerException();

    private WrongCaptchaAnswerException() {
        super(CaptchaErrorCode.WRONG_CAPTCHA_ANSWER);
    }
}
