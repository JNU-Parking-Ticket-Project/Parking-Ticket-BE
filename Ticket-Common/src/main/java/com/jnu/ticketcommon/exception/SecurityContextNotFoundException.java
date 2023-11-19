package com.jnu.ticketcommon.exception;

public class SecurityContextNotFoundException extends RecruitCodeException {
    public static final RecruitCodeException EXCEPTION = new SecurityContextNotFoundException();

    private SecurityContextNotFoundException() {
        super(GlobalErrorCode.SECURITY_CONTEXT_NOT_FOUND);
    }
}
