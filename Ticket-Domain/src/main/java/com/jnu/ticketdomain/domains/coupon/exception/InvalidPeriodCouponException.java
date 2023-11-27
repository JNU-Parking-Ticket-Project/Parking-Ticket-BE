package com.jnu.ticketdomain.domains.coupon.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class InvalidPeriodCouponException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new InvalidPeriodCouponException();

    private InvalidPeriodCouponException() {
        super(CouponErrorCode.NOT_COUPON_ISSUING_PERIOD);
    }
}
