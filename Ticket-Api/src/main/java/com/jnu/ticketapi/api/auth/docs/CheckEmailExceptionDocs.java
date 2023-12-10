package com.jnu.ticketapi.api.auth.docs;

import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.council.exception.AlreadyExistEmailException;

@ExceptionDoc
public class CheckEmailExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("이메일이 이미 존재하는 경우")
    public TicketCodeException 이메일이_이미_존재합니다 = AlreadyExistEmailException.EXCEPTION;
}
