package com.jnu.ticketdomain.domains.coupon.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import java.util.List;

@Port
public interface SectorLoadPort {
    Sector findById(Long sectorId);

    List<Sector> findAll();
}
