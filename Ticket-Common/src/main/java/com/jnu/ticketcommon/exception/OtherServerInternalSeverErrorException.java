package com.jnu.ticketcommon.exception;

public class OtherServerInternalSeverErrorException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION =
            new OtherServerInternalSeverErrorException();

    private OtherServerInternalSeverErrorException() {
        super(GlobalErrorCode.OTHER_SERVER_INTERNAL_SERVER_ERROR);
    }
}
