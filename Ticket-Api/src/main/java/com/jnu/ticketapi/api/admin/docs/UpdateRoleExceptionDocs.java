package com.jnu.ticketapi.api.admin.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;

@ExceptionDoc
public class UpdateRoleExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("권한을 변경할 유저가 존재하지 않는 경우")
    public TicketCodeException 권한을_변경할_유저가_존재하지_않음 = NotFoundUserException.EXCEPTION;
}
