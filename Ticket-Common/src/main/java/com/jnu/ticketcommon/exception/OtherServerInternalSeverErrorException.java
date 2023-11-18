package com.jnu.ticketcommon.exception;

public class OtherServerInternalSeverErrorException extends RecruitCodeException {

    public static final RecruitCodeException EXCEPTION =
            new OtherServerInternalSeverErrorException();

    private OtherServerInternalSeverErrorException() {
        super(GlobalErrorCode.OTHER_SERVER_INTERNAL_SERVER_ERROR);
    }
}
