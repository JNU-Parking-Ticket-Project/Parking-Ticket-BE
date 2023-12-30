package com.jnu.ticketapi.api.notice.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.notice.exception.NoticeErrorCode;

@ExceptionDoc
public class UpdateNoticeExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("안내사항 내용이 10,000자를 초과하는 경우")
    public TicketCodeException 안내사항_내용이_10000자를_초과함 =
            new TicketCodeException(NoticeErrorCode.NOTICE_CONTENT_EXCEEDS_LIMIT_ERROR);
}
