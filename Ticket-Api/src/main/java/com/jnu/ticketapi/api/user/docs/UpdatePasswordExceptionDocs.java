package com.jnu.ticketapi.api.user.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.user.exception.CredentialCodeNotExistException;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;

@ExceptionDoc
public class UpdatePasswordExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("비밀번호 변경할 사용자가 존재하지 않는 경우")
    public TicketCodeException 사용자가_존재하지_않음 = NotFoundUserException.EXCEPTION;

    @ExplainError("발급된 인증링크가 잘못된 경우")
    public TicketCodeException 잘못된_링크_접근 = CredentialCodeNotExistException.EXCEPTION;
}
