package com.jnu.ticketapi.api.auth.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.InvalidTokenException;
import com.jnu.ticketcommon.exception.NotEqualPrincipalException;
import com.jnu.ticketcommon.exception.NotFoundRefreshTokenException;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;

@ExceptionDoc
public class TokenReissueExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("엑세스토큰의 principal과 리프레시토큰의 principal이 일치하지 않는 경우(자신의 리프레시토큰이 아닐경우)")
    public TicketCodeException 엑세스토큰의_principal과_리프레시토큰의_principal이_일치하지_않습니다 =
            NotEqualPrincipalException.EXCEPTION;

    @ExplainError("요청으로 보낸 리프레시 토큰을 redis에서 찾을 수 없는 경우")
    public TicketCodeException 리프레시토큰을_찾을_수_없습니다 = NotFoundRefreshTokenException.EXCEPTION;

    @ExplainError("요청으로 보낸 리프레시 토큰이 유효하지 않거나, redis에 저장된 값과 다를경우")
    public TicketCodeException 리프레시토큰이_유효하지_않습니다 = InvalidTokenException.EXCEPTION;
}
