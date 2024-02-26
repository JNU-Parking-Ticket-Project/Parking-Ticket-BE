package com.jnu.ticketapi.api.event.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.GlobalErrorCode;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.CannotUpdateClosedEventException;
import com.jnu.ticketdomain.domains.events.exception.CannotUpdatePublishEventException;
import com.jnu.ticketdomain.domains.events.exception.InvalidPeriodEventException;
import com.jnu.ticketdomain.domains.events.exception.NotIssuingEventPeriodException;

@ExceptionDoc
public class UpdateEventExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("설정한 시간이 현재를 포함하고 있을 경우")
    public TicketCodeException 시작종료시간_현재를_포함 = InvalidPeriodEventException.EXCEPTION;

    @ExplainError("설정한 시간이 이미 지났을 때 발급한 경우")
    public TicketCodeException 종료시간_지남 = NotIssuingEventPeriodException.EXCEPTION;

    @ExplainError("시작 시간이 종료 시간보다 늦을 때 발급한 경우")
    public TicketCodeException 시작시간_종료시간_이후 = NotIssuingEventPeriodException.EXCEPTION;

    @ExplainError("제목이 빈칸인 경우")
    public TicketCodeException 이벤트가_PUBLISH_TURE_상태 = CannotUpdatePublishEventException.EXCEPTION;

    @ExplainError("제목이 빈칸인 경우")
    public TicketCodeException 제목이_빈칸일_때 =
            new TicketCodeException(GlobalErrorCode.TITLE_MUST_NOT_BLANK);

    @ExplainError("날짜가 null인 경우")
    public TicketCodeException 날짜가_null일_때 =
            new TicketCodeException(GlobalErrorCode.DATE_MUST_NOT_NULL);

    @ExplainError("이벤트가 종료된 상태인 경우")
    public TicketCodeException 이벤트가_종료된_상태 = CannotUpdateClosedEventException.EXCEPTION;
}
