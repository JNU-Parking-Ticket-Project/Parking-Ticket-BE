package com.jnu.ticketapi.api.event.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;

@ExceptionDoc
public class ReadPartsOfEventExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("일치하는 EVENT를 조회할 수 없는 경우")
    public TicketCodeException 일치하는_EVENT를_찾을수_없음 = NotFoundEventException.EXCEPTION;
}
