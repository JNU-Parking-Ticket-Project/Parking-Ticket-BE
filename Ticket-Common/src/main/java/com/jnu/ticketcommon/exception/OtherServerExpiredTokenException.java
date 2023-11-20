package com.jnu.ticketcommon.exception;

public class OtherServerExpiredTokenException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new OtherServerExpiredTokenException();

    private OtherServerExpiredTokenException() {
        super(GlobalErrorCode.OTHER_SERVER_EXPIRED_TOKEN);
    }
}
