package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.dto.*;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.TemporarySaveResponse;
import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;

import java.util.List;
import java.util.stream.Collectors;

@Helper
public class Converter {
    public List<SectorDto> toSectorDto(List<Sector> sectorList) {
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
    public Registration temporaryToRegistration(TemporarySaveRequest requestDto, Sector sector) {
        return Registration.builder()
                .email(requestDto.email())
                .name(requestDto.name())
                .studentNum(requestDto.studentNum())
                .affiliation(requestDto.affiliation())
                .carNum(requestDto.carNum())
                .isLight(requestDto.isLight())
                .phoneNum(requestDto.phoneNum())
                .sector(sector)
                .isSaved(false)
                .build();
    }

    public Registration finalToRegistration(FinalSaveRequestDto requestDto, Sector sector) {
        return Registration.builder()
                .email(requestDto.email())
                .name(requestDto.name())
                .studentNum(requestDto.studentNum())
                .affiliation(requestDto.affiliation())
                .carNum(requestDto.carNum())
                .isLight(requestDto.isLight())
                .phoneNum(requestDto.phoneNum())
                .sector(sector)
                .isSaved(true)
                .build();
    }

    public TemporarySaveResponse toTemporarySaveResponseDto(Registration registration) {
        return TemporarySaveResponse.builder()
                .registrationId(registration.getId())
                .message(ResponseMessage.SUCCESS_TEMPORARY_SAVE)
                .build();
    }

    public FinalSaveResponseDto toFinalSaveResponseDto(Registration registration) {
        return FinalSaveResponseDto.builder()
                .registrationId(registration.getId())
                .message(ResponseMessage.SUCCESS_FINAL_SAVE)
                .build();
    }
}
