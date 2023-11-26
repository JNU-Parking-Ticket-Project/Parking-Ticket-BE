package com.jnu.ticketdomain.domains.coupon.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Coupon;
import com.jnu.ticketdomain.domains.coupon.domain.CouponStatus;
import com.jnu.ticketdomain.domains.coupon.exception.NotFoundCouponException;
import com.jnu.ticketdomain.domains.coupon.out.CouponLoadPort;
import com.jnu.ticketdomain.domains.coupon.out.CouponRecordPort;
import com.jnu.ticketdomain.domains.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class CouponAdaptor implements CouponRecordPort, CouponLoadPort {
    private final CouponRepository couponRepository;

    public Coupon findById(Long couponId) {
        return couponRepository
                .findById(couponId)
                .orElseThrow(() -> NotFoundCouponException.EXCEPTION);
    }

    public Coupon findOpenCoupon() {
        return couponRepository
                .findByCouponStatus(CouponStatus.OPEN)
                .orElseThrow(() -> NotFoundCouponException.EXCEPTION);
    }

    @Override
    public Coupon save(Coupon coupon) {
        return couponRepository.save(coupon);
    }
}
