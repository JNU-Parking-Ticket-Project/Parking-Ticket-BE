package com.jnu.ticketcommon.exception;

public class OtherServerNotFoundException extends RecruitCodeException {
    public static final RecruitCodeException EXCEPTION = new OtherServerNotFoundException();

    private OtherServerNotFoundException() {
        super(GlobalErrorCode.OTHER_SERVER_NOT_FOUND);
    }
}
