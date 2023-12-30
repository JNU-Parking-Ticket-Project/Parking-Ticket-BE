package com.jnu.ticketapi.api.announce.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.announce.exception.AnnounceIdNotExistException;

@ExceptionDoc
public class DeleteAnnounceExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("삭제할 공지사항이 존재하지 않는 경우")
    public TicketCodeException 삭제할_공지사항이_존재하지_않음 = AnnounceIdNotExistException.EXCEPTION;
}
