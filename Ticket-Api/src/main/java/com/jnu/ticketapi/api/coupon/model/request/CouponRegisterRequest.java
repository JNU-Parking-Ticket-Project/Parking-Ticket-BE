package com.jnu.ticketapi.api.coupon.model.request;


import com.jnu.ticketapi.api.sector.model.request.SectorRegisterRequest;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import java.util.List;
import lombok.Builder;

public record CouponRegisterRequest(
        DateTimePeriod dateTimePeriod, List<SectorRegisterRequest> sectors) {
    @Builder
    public CouponRegisterRequest {}

    public List<Sector> getSectors() {
        return sectors.stream()
                .map(
                        sectorRegisterRequest ->
                                new Sector(
                                        sectorRegisterRequest.sectorNumber(),
                                        sectorRegisterRequest.name(),
                                        sectorRegisterRequest.sectorCapacity(),
                                        sectorRegisterRequest.reserve()))
                // 쿠폰 등록 로직 구현
                .toList();
    }
}
