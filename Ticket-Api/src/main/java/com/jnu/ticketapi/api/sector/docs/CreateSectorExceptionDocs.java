package com.jnu.ticketapi.api.sector.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.coupon.exception.NotFoundSectorException;

@ExceptionDoc
public class CreateSectorExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("구간을 찾을 수 없습니다.")
    public TicketCodeException 구간_찾지_못함 = NotFoundSectorException.EXCEPTION;
}
