package com.jnu.ticketapi.api.coupon.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.coupon.adaptor.CouponAdaptor;
import com.jnu.ticketdomain.domains.coupon.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Coupon;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CouponRegisterUseCase {
    private final CouponAdaptor couponAdaptor;
    private final SectorAdaptor sectorAdaptor;

    @Transactional
    public void registerCoupon(DateTimePeriod dateTimePeriod) {
        List<Sector> sectors = sectorAdaptor.findAll();
        Coupon coupon = new Coupon(dateTimePeriod, sectors);
        coupon.validateIssuePeriod();
        Coupon savedCoupon = couponAdaptor.save(coupon);
        sectors.forEach(sector -> sector.setCoupon(savedCoupon));
    }
}
