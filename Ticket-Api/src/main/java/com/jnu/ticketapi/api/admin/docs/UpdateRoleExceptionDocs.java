package com.jnu.ticketapi.api.admin.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.admin.exception.AlreadyExistAdminException;
import com.jnu.ticketdomain.domains.admin.exception.NotAllowUpdateOwnRoleException;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;

@ExceptionDoc
public class UpdateRoleExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("권한을 변경할 유저가 존재하지 않는 경우")
    public TicketCodeException 권한을_변경할_유저가_존재하지_않음 = NotFoundUserException.EXCEPTION;
    @ExplainError("관리자로 권한을 변경할 때 이미 관리자가 존재하는 경우")
    public TicketCodeException 이미_관리자가_존재함 = AlreadyExistAdminException.EXCEPTION;
    @ExplainError("자신의 권한을 변경하는 경우")
    public TicketCodeException 자신의_권한을_변경함 = NotAllowUpdateOwnRoleException.EXCEPTION;
}
