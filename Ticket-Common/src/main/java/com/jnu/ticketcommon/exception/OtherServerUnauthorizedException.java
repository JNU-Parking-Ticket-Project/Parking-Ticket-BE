package com.jnu.ticketcommon.exception;

public class OtherServerUnauthorizedException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new OtherServerUnauthorizedException();

    private OtherServerUnauthorizedException() {
        super(GlobalErrorCode.OTHER_SERVER_UNAUTHORIZED);
    }
}
