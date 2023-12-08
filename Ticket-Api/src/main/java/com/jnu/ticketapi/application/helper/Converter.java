package com.jnu.ticketapi.application.helper;


import com.jnu.ticketapi.api.auth.model.internal.TokenDto;
import com.jnu.ticketapi.api.auth.model.response.LoginCouncilResponse;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.FinalSaveResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationResponse;
import com.jnu.ticketapi.api.registration.model.response.TemporarySaveResponse;
import com.jnu.ticketapi.api.sector.model.request.SectorReadRequest;
import com.jnu.ticketcommon.annotation.Helper;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import java.util.List;
import java.util.stream.Collectors;

@Helper
public class Converter {
    public List<SectorReadRequest> toSectorDto(List<Sector> sectorList) {
        return sectorList.stream()
                .map(
                        sector ->
                                SectorReadRequest.builder()
                                        .sectorId(sector.getId())
                                        .sectorNum(sector.getSectorNumber())
                                        .sectorName(sector.getName())
                                        .build())
                .collect(Collectors.toList());
    }

    public GetRegistrationResponse toGetRegistrationResponseDto(
            String email,
            Registration registration,
            List<SectorReadRequest> sectorReadRequestList) {
        return GetRegistrationResponse.builder()
                .email(email)
                .name(registration.getName())
                .studentNum(registration.getStudentNum())
                .affiliation(registration.getAffiliation())
                .carNum(registration.getCarNum())
                .isLight(registration.isLight())
                .phoneNum(registration.getPhoneNum())
                .sectors(sectorReadRequestList)
                .selectSectorId(registration.getSector().getId())
                .build();
    }

    public Registration temporaryToRegistration(
            TemporarySaveRequest requestDto, Sector sector, String email, User user) {
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
                .user(user)
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

    public LoginCouncilResponse toLoginCouncilResponseDto(TokenDto tokenDto) {
        return LoginCouncilResponse.builder()
                .accessToken(tokenDto.accessToken())
                .refreshToken(tokenDto.refreshToken())
                .build();
    }
}
