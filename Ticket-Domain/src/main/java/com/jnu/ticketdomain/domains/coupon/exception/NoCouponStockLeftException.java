package com.jnu.ticketdomain.domains.coupon.exception;


import com.jnu.ticketcommon.exception.TicketCodeException;

public class NoCouponStockLeftException extends TicketCodeException {
    public static final TicketCodeException EXCEPTION = new NoCouponStockLeftException();

    private NoCouponStockLeftException() {
        super(CouponErrorCode.NO_COUPON_STOCK_LEFT);
    }
}
