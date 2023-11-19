package com.jnu.ticketcommon.exception;

public class RefreshTokenExpiredException extends RecruitCodeException {
    public static final RecruitCodeException EXCEPTION = new RefreshTokenExpiredException();

    private RefreshTokenExpiredException() {
        super(GlobalErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}
