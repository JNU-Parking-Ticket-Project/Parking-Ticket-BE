package com.jnu.ticketapi.api.auth.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.BadCredentialException;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.council.exception.IsNotCouncilException;

@ExceptionDoc
public class CouncilLoginExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("로그인 할 때 비밀번호가 일치하지 않는 경우")
    public TicketCodeException 비밀번호가_일치하지_않습니다 = BadCredentialException.EXCEPTION;

    @ExplainError("사용자의 권한이 학생회 이상이 아닐경우")
    public TicketCodeException 권한이_학생회_이상이_아닙나다 = IsNotCouncilException.EXCEPTION;

    @ExplainError("이메일 형식이 올바르지 않는 경우")
    public TicketCodeException 이메일_형식이_올바르지_않습니다 =
            new TicketCodeException(GlobalErrorCode.EMAIL_NOT_VALID);

    @ExplainError("비밀번호가 정규식에 맞지 않는 경우")
    public TicketCodeException 비밀번호가_정규식에_맞지_않습니다 =
            new TicketCodeException(GlobalErrorCode.PASSWORD_NOT_VALID);
}
