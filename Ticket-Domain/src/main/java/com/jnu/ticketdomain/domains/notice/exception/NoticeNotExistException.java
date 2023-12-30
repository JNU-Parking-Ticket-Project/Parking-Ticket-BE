package com.jnu.ticketdomain.domains.notice.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NoticeNotExistException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NoticeNotExistException();

    private NoticeNotExistException() {
        super(NoticeErrorCode.NOTICE_NOT_EXIST_ERROR);
    }
}
