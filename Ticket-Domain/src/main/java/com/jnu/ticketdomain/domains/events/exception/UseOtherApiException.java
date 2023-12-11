package com.jnu.ticketdomain.domains.events.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class UseOtherApiException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new UseOtherApiException();

    private UseOtherApiException() {
        super(EventErrorCode.USE_OTHER_API);
    }
}
