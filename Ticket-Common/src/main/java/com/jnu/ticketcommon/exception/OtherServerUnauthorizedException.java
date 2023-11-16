package com.jnu.ticketcommon.exception;

public class OtherServerUnauthorizedException extends RecruitCodeException {

    public static final RecruitCodeException EXCEPTION = new OtherServerUnauthorizedException();

    private OtherServerUnauthorizedException() {
        super(GlobalErrorCode.OTHER_SERVER_UNAUTHORIZED);
    }
}
