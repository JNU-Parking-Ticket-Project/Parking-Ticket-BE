package com.jnu.ticketcommon.exception;

public class ExpiredTokenException extends RecruitCodeException {
    public static final RecruitCodeException EXCEPTION = new ExpiredTokenException();

    private ExpiredTokenException() {
        super(GlobalErrorCode.TOKEN_EXPIRED);
    }
}
