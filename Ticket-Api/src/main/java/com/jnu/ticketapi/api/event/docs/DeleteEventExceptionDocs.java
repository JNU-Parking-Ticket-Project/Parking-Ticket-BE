package com.jnu.ticketapi.api.event.docs;

import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;

@ExceptionDoc
public class DeleteEventExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("삭제할 이벤트가 없는 경우")
    public TicketCodeException 삭제할_이벤트가_없음 = NotFoundEventException.EXCEPTION;
}
