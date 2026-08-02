package com.jnu.ticketapi.api.council.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.email.exception.FailedEmailOutboxNotFoundException;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;

@ExceptionDoc
public class EmailOutboxRecoveryExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("이벤트가 존재하지 않는 경우")
    public TicketCodeException 이벤트를_찾을_수_없습니다 = NotFoundEventException.EXCEPTION;

    @ExplainError("Outbox가 최종 실패 상태가 아니거나 이미 재처리된 경우")
    public TicketCodeException 재처리할_Outbox를_찾을_수_없습니다 =
            FailedEmailOutboxNotFoundException.EXCEPTION;
}
