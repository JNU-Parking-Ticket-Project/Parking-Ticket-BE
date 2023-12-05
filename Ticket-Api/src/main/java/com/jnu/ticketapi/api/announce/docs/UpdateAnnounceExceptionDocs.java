package com.jnu.ticketapi.api.announce.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.AnnounceNotExistException;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;

@ExceptionDoc
public class UpdateAnnounceExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("업데이트 할 공지사항이 존재하지 않는 경우")
    public TicketCodeException 업데이트할_공지사항이_존재하지_않음 = AnnounceNotExistException.EXCEPTION;
}
