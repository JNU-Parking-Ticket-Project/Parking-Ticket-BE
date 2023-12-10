package com.jnu.ticketapi.api.auth.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.BadCredentialException;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;

@ExceptionDoc
public class UserLoginExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("로그인 할 때 비밀번호가 일치하지 않는 경우")
    public TicketCodeException 비밀번호가_일치하지_않습니다 = BadCredentialException.EXCEPTION;
}
