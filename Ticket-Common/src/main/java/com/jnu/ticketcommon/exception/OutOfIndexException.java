package com.jnu.ticketcommon.exception;

public class OutOfIndexException extends RecruitCodeException {
    public static final RecruitCodeException EXCEPTION = new OutOfIndexException();

    private OutOfIndexException() {
        super(GlobalErrorCode.OUT_OF_INDEX);
    }
}
