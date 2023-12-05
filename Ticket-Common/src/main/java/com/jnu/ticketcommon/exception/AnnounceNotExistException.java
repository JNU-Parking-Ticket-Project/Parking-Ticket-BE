package com.jnu.ticketcommon.exception;

public class AnnounceNotExistException extends TicketCodeException{
    public static final TicketCodeException EXCEPTION = new AnnounceNotExistException();

    private AnnounceNotExistException(){ super(GlobalErrorCode.ANNOUNCE_NOT_EXIST_ERROR);}
}
