package com.jnu.ticketapi.api.event.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.NotReadyEventStatusException;

@ExceptionDoc
public class ReadEventPeriodExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("READY_상태만_조회가능합니다.")
    public TicketCodeException READY_상태만_가능 = NotReadyEventStatusException.EXCEPTION;
}
