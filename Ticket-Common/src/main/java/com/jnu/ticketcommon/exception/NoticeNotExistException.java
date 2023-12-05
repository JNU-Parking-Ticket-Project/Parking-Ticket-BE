package com.jnu.ticketcommon.exception;

public class NoticeNotExistException extends TicketCodeException{
    public static final TicketCodeException EXCEPTION = new NoticeNotExistException();

    private NoticeNotExistException(){ super(GlobalErrorCode.NOTICE_NOT_EXIST_ERROR);}
}
