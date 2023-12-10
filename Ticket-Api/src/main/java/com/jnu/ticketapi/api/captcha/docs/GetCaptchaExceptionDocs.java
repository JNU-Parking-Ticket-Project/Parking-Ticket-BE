package com.jnu.ticketapi.api.captcha.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.EncryptionErrorException;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;

@ExceptionDoc
public class GetCaptchaExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("captchaPendingId(PK)를 암호화 하는 도중 에러가 발생한 경우")
    public TicketCodeException 암호화_도중_에러가_발생하였습니다 = EncryptionErrorException.EXCEPTION;
}
