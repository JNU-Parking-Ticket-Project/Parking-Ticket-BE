package com.jnu.ticketapi.api.council.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;

@ExceptionDoc
public class EmailOutboxQueryExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("이벤트가 존재하지 않는 경우")
    public TicketCodeException 이벤트를_찾을_수_없습니다 = NotFoundEventException.EXCEPTION;
}
