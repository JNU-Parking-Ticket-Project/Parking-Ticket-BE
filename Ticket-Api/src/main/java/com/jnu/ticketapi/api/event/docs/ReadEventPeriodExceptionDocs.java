package com.jnu.ticketapi.api.event.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.AlreadyCloseStatusException;
import com.jnu.ticketdomain.domains.events.exception.NotPublishEventException;

@ExceptionDoc
public class ReadEventPeriodExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("이벤트가 모두 마감된 경우")
    public TicketCodeException 이벤트가_모두_마감된_경우 = AlreadyCloseStatusException.EXCEPTION;

    @ExplainError("게시된 이벤트가 없을 경우")
    public TicketCodeException 게시된_이벤트가_없을_경우 = NotPublishEventException.EXCEPTION;
}
