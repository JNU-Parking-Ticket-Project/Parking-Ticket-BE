package com.jnu.ticketapi.api.council.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.council.exception.AlreadyExistEmailException;
import com.jnu.ticketdomain.domains.council.exception.CouncilErrorCode;

@ExceptionDoc
public class CouncilSignUpExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("이메일이 이미 존재하는 경우")
    public TicketCodeException 이메일이_이미_존재합니다 = AlreadyExistEmailException.EXCEPTION;
    @ExplainError("이메일 형식이 올바르지 않는 경우")
    public TicketCodeException 이메일_형식이_올바르지_않습니다 = new TicketCodeException(GlobalErrorCode.EMAIL_NOT_VALID);
    @ExplainError("비밀번호가 정규식에 맞지 않는 경우")
    public TicketCodeException 비밀번호가_정규식에_맞지_않습니다 = new TicketCodeException(GlobalErrorCode.PASSWORD_NOT_VALID);
    @ExplainError("이름에 null 혹은 빈칸인 경우")
    public TicketCodeException 이름에_null_혹은_빈칸인_경우 = new TicketCodeException(GlobalErrorCode.NAME_MUST_NOT_BLANK);
    @ExplainError("전화번호 형식이 올바르지 않는 경우")
    public TicketCodeException 전화번호_형식이_올바르지_않는_경우 = new TicketCodeException(GlobalErrorCode.PHONE_NUMBER_NOT_VALID);
    @ExplainError("학번에 null 혹은 빈칸인 경우")
    public TicketCodeException 학번에_null_혹은_빈칸인_경우 = new TicketCodeException(GlobalErrorCode.STUDENT_NUMBER_MUST_NOT_BLANK);
}
