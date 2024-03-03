package com.jnu.ticketdomain.domains.events.out;


import com.jnu.ticketcommon.annotation.Port;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import java.util.List;

@Port
public interface SectorLoadPort {
    Sector findById(Long sectorId);

    List<Sector> findAll();

    List<Sector> findByEventId(Long eventId);

    List<Sector> findRecentSector();

    List<Sector> findAllByEventStatusAndPublishAndIsDeleted();

    Sector findByIdAndPublishIsFalse(Long sectorId);
}
