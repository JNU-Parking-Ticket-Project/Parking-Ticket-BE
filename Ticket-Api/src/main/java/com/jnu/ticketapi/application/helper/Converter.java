package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.dto.SectorDto;
import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import java.util.List;
import java.util.stream.Collectors;

@Helper
public class Converter {
    public List<SectorDto> sectorToDto(List<Sector> sectorList) {
        return sectorList.stream()
                .map(
                        (sector) ->
                                SectorDto.builder()
                                        .sectorId(sector.getId())
                                        .sectorName(sector.getSectorNumber())
                                        .sectionColleges(sector.getName())
                                        .build())
                .collect(Collectors.toList());
    }
}
