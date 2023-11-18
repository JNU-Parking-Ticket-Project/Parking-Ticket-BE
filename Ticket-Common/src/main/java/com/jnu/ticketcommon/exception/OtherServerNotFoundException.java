package com.jnu.ticketcommon.exception;

public class OtherServerNotFoundException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new OtherServerNotFoundException();

    private OtherServerNotFoundException() {
        super(GlobalErrorCode.OTHER_SERVER_NOT_FOUND);
    }
}
