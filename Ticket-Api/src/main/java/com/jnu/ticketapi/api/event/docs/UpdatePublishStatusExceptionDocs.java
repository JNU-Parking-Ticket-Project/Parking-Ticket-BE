package com.jnu.ticketapi.api.event.docs;

import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.events.exception.AlreadyExistPublishEventException;
import com.jnu.ticketdomain.domains.events.exception.AlreadyPublishEventException;
import com.jnu.ticketdomain.domains.events.exception.AlreadyUnpublishEventException;
import com.jnu.ticketdomain.domains.events.exception.CannotPublishClosedEventException;

@ExceptionDoc
public class UpdatePublishStatusExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("이미 게시된 이벤트를 게시 하려는 경우")
    public TicketCodeException 이미_게시된_이벤트 = AlreadyPublishEventException.EXCEPTION;

    @ExplainError("이미 미게시 이벤트를 미게시 하려는 경우")
    public TicketCodeException 이미_미게시_이벤트 = AlreadyUnpublishEventException.EXCEPTION;

    @ExplainError("이미 게시된 이벤트가 존재하는데 게시하려고 하는 경우")
    public TicketCodeException 이미_게시된_이벤트가_존재 = AlreadyExistPublishEventException.EXCEPTION;

    @ExplainError("이미 종료된 이벤트를 게시하려는 경우")
    public TicketCodeException 종료된_이벤트를_게시 = CannotPublishClosedEventException.EXCEPTION;
}
