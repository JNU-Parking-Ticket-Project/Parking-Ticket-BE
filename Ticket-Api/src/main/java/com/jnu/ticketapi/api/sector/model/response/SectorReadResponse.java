package com.jnu.ticketapi.api.sector.model.response;


import com.jnu.ticketdomain.domains.events.domain.Sector;
import java.util.List;
import lombok.Builder;

public record SectorReadResponse(
        Long id,
        String sectorNumber,
        String name,
        Integer sectorCapacity,
        Integer reserve,
        Integer issueAmount) {

    @Builder
    public SectorReadResponse {}

    public static List<SectorReadResponse> toSectorReadResponses(List<Sector> all) {
        return all.stream()
                .map(
                        sector ->
                                new SectorReadResponse(
                                        sector.getId(),
                                        sector.getSectorNumber(),
                                        sector.getName(),
                                        sector.getSectorCapacity(),
                                        sector.getReserve(),
                                        sector.getIssueAmount()))
                .toList();
    }
}
