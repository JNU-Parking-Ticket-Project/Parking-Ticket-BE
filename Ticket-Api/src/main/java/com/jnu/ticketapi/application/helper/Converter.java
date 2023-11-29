package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.dto.GetRegistrationResponseDto;
import com.jnu.ticketapi.dto.SectorDto;
import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;

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
    public GetRegistrationResponseDto toGetRegistrationResponseDto (String email, Registration registration, List<SectorDto> sectorDtoList) {
        return GetRegistrationResponseDto.builder()
                .email(email)
                .name(registration.getName())
                .studentNum(registration.getStudentNum())
                .affiliation(registration.getAffiliation())
                .carNum(registration.getCarNum())
                .isLight(registration.isLight())
                .phoneNum(registration.getPhoneNum())
                .sector(sectorDtoList)
                .selectSectorId(registration.getSector().getId())
                .build();
    }


}
