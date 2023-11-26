package com.jnu.ticketdomain.domains.coupon.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import java.util.List;

@Port
public interface SectorRecordPort {
    public Sector save(Sector coupon);

    void saveAll(List<Sector> sectorList);

    void updateAll(List<Sector> prevSector, List<Sector> sectorList);
}
