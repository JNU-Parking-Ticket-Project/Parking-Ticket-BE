package com.jnu.ticketdomain.domains.events.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import java.util.List;

@Port
public interface SectorRecordPort {
    Sector save(Sector coupon);

    List<Sector> saveAll(List<Sector> sectorList);

    void updateAll(List<Sector> prevSector, List<Sector> sectorList);

    void delete(Long sectorId);

    void deleteByEvent(Long eventId);
}
