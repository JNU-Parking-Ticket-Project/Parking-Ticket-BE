package com.jnu.ticketdomain.domains.coupon.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.coupon.domain.Coupon;

@Port
public interface CouponRecordPort {
    Coupon save(Coupon coupon);
}
