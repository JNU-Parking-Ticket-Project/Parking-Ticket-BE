package com.jnu.ticketapi.api.user.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;

@ExceptionDoc
public class SendEmailExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("메일을 전송할 기존 사용자가 존재하지 않는 경우")
    public TicketCodeException 사용자가_존재하지_않음 = NotFoundUserException.EXCEPTION;
}
