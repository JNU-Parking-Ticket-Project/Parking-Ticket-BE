package com.jnu.ticketcommon.exception;

public class BadLockIdentifierException extends RecruitCodeException {

    public static final RecruitCodeException EXCEPTION = new BadLockIdentifierException();

    private BadLockIdentifierException() {
        super(GlobalErrorCode.BAD_LOCK_IDENTIFIER);
    }
}
