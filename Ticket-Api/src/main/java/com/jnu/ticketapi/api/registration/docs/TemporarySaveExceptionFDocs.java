package com.jnu.ticketapi.api.registration.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;

@ExceptionDoc
public class TemporarySaveExceptionFDocs implements SwaggerExampleExceptions {
    @ExplainError("요청하는 사용자를 찾을 수 없는 경우")
    public TicketCodeException 사용자를_찾을_수_없습니다 = NotFoundUserException.EXCEPTION;

    @ExplainError("selectSectorId에 해당 하는 구간을 찾을 수 없는 경우")
    public TicketCodeException 구간을_찾을_수_없습니다 = NotFoundSectorException.EXCEPTION;
}
