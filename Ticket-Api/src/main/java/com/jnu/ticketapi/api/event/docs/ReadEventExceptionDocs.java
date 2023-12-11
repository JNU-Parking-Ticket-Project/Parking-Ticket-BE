package com.jnu.ticketapi.api.event.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketdomain.domains.events.exception.NotOpenEventPeriodException;

@ExceptionDoc
public class ReadEventExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("발급하지 않은 쿠폰을 조회할 경우")
    public TicketCodeException 미발급_쿠폰_조회 = NotFoundEventException.EXCEPTION;

    @ExplainError("쿠폰이 아직 오픈되지 않은 경우")
    public TicketCodeException 쿠폰_미오픈 = NotOpenEventPeriodException.EXCEPTION;
}
