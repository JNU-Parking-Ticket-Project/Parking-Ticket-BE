package com.jnu.ticketdomain.domains.announce.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class AnnounceIdNotExistException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AnnounceIdNotExistException();

    private AnnounceIdNotExistException() {
        super(AnnounceErrorCode.ANNOUNCE_ID_NOT_EXIST_ERROR);
    }
}
