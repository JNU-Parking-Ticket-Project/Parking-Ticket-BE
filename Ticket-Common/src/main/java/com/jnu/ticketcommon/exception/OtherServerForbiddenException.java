package com.jnu.ticketcommon.exception;

public class OtherServerForbiddenException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new OtherServerForbiddenException();

    private OtherServerForbiddenException() {
        super(GlobalErrorCode.OTHER_SERVER_FORBIDDEN);
    }
}
