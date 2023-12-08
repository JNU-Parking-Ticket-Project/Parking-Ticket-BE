package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.api.auth.model.internal.TokenDto;
import com.jnu.ticketapi.api.auth.model.response.LoginCouncilResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationResponse;
import com.jnu.ticketapi.api.sector.model.internal.SectorDto;
import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.List;
import java.util.stream.Collectors;

@Helper
public class Converter {
    public List<SectorDto> toSectorDto(List<Sector> sectorList) {
        return sectorList.stream()
                .map(
                        sector ->
                                SectorDto.builder()
                                        .sectorId(sector.getId())
                                        .sectorNum(sector.getSectorNumber())
                                        .sectorName(sector.getName())
                                        .build())
                .collect(Collectors.toList());
    }

    public GetRegistrationResponse toGetRegistrationResponseDto(
            String email, Registration registration, List<SectorDto> sectorDtoList) {
        return GetRegistrationResponse.builder()
                .email(email)
                .name(registration.getName())
                .studentNum(registration.getStudentNum())
                .affiliation(registration.getAffiliation())
                .carNum(registration.getCarNum())
                .isLight(registration.isLight())
                .phoneNum(registration.getPhoneNum())
                .sectors(sectorDtoList)
                .selectSectorId(registration.getSector().getId())
                .build();
    }

    public LoginCouncilResponse toLoginCouncilResponseDto(TokenDto tokenDto) {
        return LoginCouncilResponse.builder()
                .accessToken(tokenDto.accessToken())
                .refreshToken(tokenDto.refreshToken())
                .build();
    }
}
