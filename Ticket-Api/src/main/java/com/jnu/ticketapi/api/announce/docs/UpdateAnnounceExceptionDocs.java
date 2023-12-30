package com.jnu.ticketapi.api.announce.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.announce.exception.AnnounceErrorCode;
import com.jnu.ticketdomain.domains.announce.exception.AnnounceNotExistException;

@ExceptionDoc
public class UpdateAnnounceExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("업데이트 할 공지사항이 존재하지 않는 경우")
    public TicketCodeException 업데이트할_공지사항이_존재하지_않음 = AnnounceNotExistException.EXCEPTION;

    @ExplainError("공지사항 내용이 10,000자를 초과하는 경우")
    public TicketCodeException 공지사항_내용이_10000자를_초과함 =
            new TicketCodeException(AnnounceErrorCode.ANNOUNCE_CONTENT_EXCEEDS_LIMIT_ERROR);

    @ExplainError("공지사항 제목이 100자를 초과하는 경우")
    public TicketCodeException 공지사항_제목이_100자를_초과함 =
            new TicketCodeException(AnnounceErrorCode.ANNOUNCE_TITLE_EXCEEDS_LIMIT_ERROR);
}
