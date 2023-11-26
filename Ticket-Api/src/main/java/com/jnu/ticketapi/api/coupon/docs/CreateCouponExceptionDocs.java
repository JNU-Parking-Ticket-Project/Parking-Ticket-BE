package com.jnu.ticketapi.api.coupon.docs;


import com.jnu.ticketcommon.annotation.ExceptionDoc;
import com.jnu.ticketcommon.annotation.ExplainError;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketcommon.interfaces.SwaggerExampleExceptions;
import com.jnu.ticketdomain.domains.coupon.exception.InvalidPeriodCouponException;
import com.jnu.ticketdomain.domains.coupon.exception.NotIssuingCouponPeriodException;

@ExceptionDoc
public class CreateCouponExceptionDocs implements SwaggerExampleExceptions {

    @ExplainError("설정한 시간이 현재를 포함하고 있을 경우")
    public TicketCodeException 시작종료시간_현재를_포함 = InvalidPeriodCouponException.EXCEPTION;

    @ExplainError("설정한 시간이 이미 지났을 때 발급한 경우")
    public TicketCodeException 종료시간_지남 = NotIssuingCouponPeriodException.EXCEPTION;

    @ExplainError("시작 시간이 종료 시간보다 늦을 때 발급한 경우")
    public TicketCodeException 시작시간_종료시간_이후 = NotIssuingCouponPeriodException.EXCEPTION;
}
