package com.jnu.ticketcommon.exception;

public class AnnounceIdNotExistException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new AnnounceIdNotExistException();

    private AnnounceIdNotExistException() {
        super(GlobalErrorCode.ANNOUNCE_ID_NOT_EXIST_ERROR);
    }
}
