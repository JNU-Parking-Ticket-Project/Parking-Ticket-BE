package com.jnu.ticketapi.api.council.docs;

import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.council.exception.SendFailEmailException;

@ExceptionDoc
public class CouncilSendEmailException implements SwaggerExampleExceptions {
    @ExplainError("이메일 전송 요청이 실패한 경우")
    public TicketCodeException 이메일_전송이_실패하였습니다 = SendFailEmailException.EXCEPTION;
}
