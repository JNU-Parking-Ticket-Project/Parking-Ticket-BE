package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.FinalSaveResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationResponse;
import com.jnu.ticketapi.api.registration.model.response.TemporarySaveResponse;
import com.jnu.ticketapi.api.user.model.response.UpdateRoleResponse;
import com.jnu.ticketapi.dto.*;
import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
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
                .sector(sectorDtoList)
                .selectSectorId(registration.getSector().getId())
                .build();
    }

    public Registration temporaryToRegistration(
            TemporarySaveRequest requestDto, Sector sector, String email) {
        return Registration.builder()
                .email(email)
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

    public Registration finalToRegistration(
            FinalSaveRequest requestDto, Sector sector, String email, User user) {
        return Registration.builder()
                .email(email)
                .name(requestDto.name())
                .studentNum(requestDto.studentNum())
                .affiliation(requestDto.affiliation())
                .carNum(requestDto.carNum())
                .isLight(requestDto.isLight())
                .phoneNum(requestDto.phoneNum())
                .sector(sector)
                .isSaved(true)
                .user(user)
                .build();
    }

    public TemporarySaveResponse toTemporarySaveResponseDto(Registration registration) {
        return TemporarySaveResponse.builder()
                .registrationId(registration.getId())
                .message(ResponseMessage.SUCCESS_TEMPORARY_SAVE)
                .build();
    }

    public FinalSaveResponse toFinalSaveResponseDto(Registration registration) {
        return FinalSaveResponse.builder()
                .registrationId(registration.getId())
                .message(ResponseMessage.SUCCESS_FINAL_SAVE)
                .build();
    }

    public UpdateRoleResponse toUpdateRoleResponseDto(User user) {
        return UpdateRoleResponse.builder()
                .userId(user.getId())
                .role(user.getUserRole().toString())
                .build();
    }
}
