package com.jnu.ticketdomain.domains.coupon.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NotFoundSectorException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NotFoundSectorException();

    private NotFoundSectorException() {
        super(CouponErrorCode.NOT_FOUND_COUPON);
    }
}
