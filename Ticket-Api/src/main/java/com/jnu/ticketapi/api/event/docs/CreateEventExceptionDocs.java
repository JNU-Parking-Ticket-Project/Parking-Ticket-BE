package com.jnu.ticketapi.api.event.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.InvalidPeriodEventException;
import com.jnu.ticketdomain.domains.events.exception.NotIssuingEventPeriodException;

@ExceptionDoc
public class CreateEventExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("설정한 시작 시간이 현재시간보다 과거인 경우")
    public TicketCodeException 시작시간_현재시간보다_과거 = InvalidPeriodEventException.EXCEPTION;

    @ExplainError("설정한 종료 시간이 현재시간보다 과거인 경우")
    public TicketCodeException 종료시간_현재시간보다_과거 = InvalidPeriodEventException.EXCEPTION;

    @ExplainError("시작 시간이 종료 시간보다 미래로 설정한 경우")
    public TicketCodeException 시작시간이_종료시간_이후 = NotIssuingEventPeriodException.EXCEPTION;

    @ExplainError("제목이 빈칸인 경우")
    public TicketCodeException 제목이_빈칸일_때 =
            new TicketCodeException(GlobalErrorCode.TITLE_MUST_NOT_BLANK);

    @ExplainError("날짜가 null인 경우")
    public TicketCodeException 날짜가_null일_때 =
            new TicketCodeException(GlobalErrorCode.DATE_MUST_NOT_NULL);
}
