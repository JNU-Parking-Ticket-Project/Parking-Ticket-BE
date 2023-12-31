package com.jnu.ticketapi.api.announce.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.announce.exception.AnnounceErrorCode;

@ExceptionDoc
public class SaveAnnounceExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("공지사항 내용이 10,000자를 초과하는 경우")
    public TicketCodeException 공지사항_내용이_10000자를_초과함 =
            new TicketCodeException(AnnounceErrorCode.ANNOUNCE_CONTENT_EXCEEDS_LIMIT_ERROR);

    @ExplainError("공지사항 제목이 100자를 초과하는 경우")
    public TicketCodeException 공지사항_제목이_100자를_초과함 =
            new TicketCodeException(AnnounceErrorCode.ANNOUNCE_TITLE_EXCEEDS_LIMIT_ERROR);
}
