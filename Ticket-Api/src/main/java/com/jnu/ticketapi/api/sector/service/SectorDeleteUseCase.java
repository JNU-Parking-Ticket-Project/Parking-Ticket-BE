package com.jnu.ticketapi.api.sector.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.coupon.out.SectorLoadPort;
import com.jnu.ticketdomain.domains.coupon.out.SectorRecordPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SectorDeleteUseCase {
    private final SectorRecordPort sectorRecordPort;
    private final SectorLoadPort sectorLoadPort;

    public void execute(Long sectorId) {
        // to Sector List
        Sector sector = sectorLoadPort.findById(sectorId);
        sectorRecordPort.delete(sector);
    }
}
