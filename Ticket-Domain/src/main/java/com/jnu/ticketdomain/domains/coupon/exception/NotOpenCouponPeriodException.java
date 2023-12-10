package com.jnu.ticketdomain.domains.coupon.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotOpenCouponPeriodException extends TicketCodeException {

    public static final TicketCodeException EXCEPTION = new NotOpenCouponPeriodException();

    private NotOpenCouponPeriodException() {
        super(CouponErrorCode.NOT_COUPON_OPENING_PERIOD);
    }
}
