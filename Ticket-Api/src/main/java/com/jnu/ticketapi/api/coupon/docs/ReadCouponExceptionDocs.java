package com.jnu.ticketapi.api.coupon.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.coupon.exception.NotFoundCouponException;

@ExceptionDoc
public class ReadCouponExceptionDocs implements SwaggerExampleExceptions {
    @ExplainError("발급하지 않은 쿠폰을 조회할 경우")
    public TicketCodeException 미발급_쿠폰_조회 = NotFoundCouponException.EXCEPTION;
}
