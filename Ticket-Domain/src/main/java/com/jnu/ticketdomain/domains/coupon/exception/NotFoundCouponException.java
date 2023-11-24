package com.jnu.ticketdomain.domains.coupon.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundCouponException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundCouponException();

    private NotFoundCouponException() {
        super(CouponErrorCode.NOT_FOUND_COUPON);
    }
}
