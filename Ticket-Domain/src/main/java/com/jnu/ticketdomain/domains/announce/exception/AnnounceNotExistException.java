package com.jnu.ticketdomain.domains.announce.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AnnounceNotExistException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AnnounceNotExistException();

    private AnnounceNotExistException() {
        super(AnnounceErrorCode.ANNOUNCE_NOT_EXIST_ERROR);
    }
}
