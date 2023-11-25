package com.jnu.ticketapi.api.coupon.model.request;


import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import java.util.List;
import lombok.Builder;

public record CouponRegisterRequest(DateTimePeriod dateTimePeriod, List<Sector> sectors) {
    @Builder
    public CouponRegisterRequest {}
}
