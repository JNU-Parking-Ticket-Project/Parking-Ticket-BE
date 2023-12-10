package com.jnu.ticketapi.api.registration.model.request;


import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import lombok.Builder;

@Builder
public record FinalSaveRequest(
        String name,
        String studentNum,
        String affiliation,
        String carNum,
        boolean isLight,
        String phoneNum,
        Long selectSectorId,
        String captchaPendingCode,
        String captchaAnswer) {

    public Registration toEntity(
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
}
