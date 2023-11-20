package com.jnu.ticketcommon.exception;

public class OutOfIndexException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new OutOfIndexException();

    private OutOfIndexException() {
        super(GlobalErrorCode.OUT_OF_INDEX);
    }
}
