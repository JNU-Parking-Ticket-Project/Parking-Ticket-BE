package com.jnu.ticketapi.api.announce.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.announce.exception.AnnounceNotExistException;

@ExceptionDoc
public class GetAnnounceExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("조회할 공지사항이 존재하지 않는 경우")
    public TicketCodeException 조회할_공지사항이_존재하지_않음 = AnnounceNotExistException.EXCEPTION;
}
