package com.jnu.ticketapi.api.sector.service;


import com.jnu.ticketapi.api.sector.model.response.SectorReadResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.out.SectorLoadPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SectorReadUseCase {
    private final SectorLoadPort sectorLoadPort;

    @Transactional(readOnly = true)
    public List<SectorReadResponse> findAll() {
        List<Sector> all = sectorLoadPort.findAll();
        return SectorReadResponse.toSectorReadResponses(all);
    }

    @Transactional(readOnly = true)
    public List<SectorReadResponse> findByEventId(Long eventId) {
        List<Sector> sectors = sectorLoadPort.findByEventId(eventId);
        return SectorReadResponse.toSectorReadResponses(sectors);
    }

    @Transactional(readOnly = true)
    public List<SectorReadResponse> findRecentSector() {
        List<Sector> sectors = sectorLoadPort.findRecentSector();
        return SectorReadResponse.toSectorReadResponses(sectors);
    }
}
