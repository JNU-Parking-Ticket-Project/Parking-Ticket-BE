package com.jnu.ticketdomain.domains.coupon.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotIssuingCouponPeriodException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotIssuingCouponPeriodException();

    private NotIssuingCouponPeriodException() {
        super(CouponErrorCode.NOT_COUPON_ISSUING_PERIOD);
    }
}
