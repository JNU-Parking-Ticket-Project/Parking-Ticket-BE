package com.jnu.ticketdomain.domains.coupon.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Coupon;
import com.jnu.ticketdomain.domains.coupon.exception.NotFoundCouponException;
import com.jnu.ticketdomain.domains.coupon.repository.CouponRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class CouponAdaptor {
    private final CouponRepository couponRepository;

    public Coupon findById(Long couponId) {
        Optional<Coupon> coupon = couponRepository.findById(couponId);
        if (coupon.isEmpty()) {
            throw NotFoundCouponException.EXCEPTION;
        }
        return coupon.get();
    }
}
