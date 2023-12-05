package com.jnu.ticketapi.api.notice.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.NoticeNotExistException;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;

@ExceptionDoc
public class GetNoticeExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("조회할 안내사항이 존재하지 않는 경우")
    public TicketCodeException 조회할_안내사항이_존재하지_않음 = NoticeNotExistException.EXCEPTION;
}
