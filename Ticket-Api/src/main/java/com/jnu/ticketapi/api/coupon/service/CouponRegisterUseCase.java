package com.jnu.ticketapi.api.coupon.service;


import com.jnu.ticketapi.api.coupon.model.request.CouponRegisterRequest;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.coupon.adaptor.CouponAdaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Coupon;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CouponRegisterUseCase {
    private final CouponAdaptor couponAdaptor;
    private final WaitingQueueService waitingQueueService;
    @Transactional
    public void registerCoupon(CouponRegisterRequest couponRegisterRequest) {
        List<Sector> sectors = couponRegisterRequest.getSectors();
        Coupon coupon = new Coupon(couponRegisterRequest.dateTimePeriod(), sectors);
        coupon.validateIssuePeriod();
        couponAdaptor.save(coupon);
    }
}
