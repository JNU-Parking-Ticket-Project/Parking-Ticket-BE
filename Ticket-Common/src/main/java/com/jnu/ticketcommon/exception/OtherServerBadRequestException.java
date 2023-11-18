package com.jnu.ticketcommon.exception;

public class OtherServerBadRequestException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new OtherServerBadRequestException();

    private OtherServerBadRequestException() {
        super(GlobalErrorCode.OTHER_SERVER_BAD_REQUEST);
    }
}
