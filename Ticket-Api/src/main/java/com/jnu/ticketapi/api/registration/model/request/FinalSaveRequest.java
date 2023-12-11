package com.jnu.ticketapi.api.registration.model.request;


import com.jnu.ticketcommon.annotation.Phone;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record FinalSaveRequest(
        @Schema(defaultValue = "홍길동", description = "이름") String name,
        @Schema(defaultValue = "183027", description = "학번") String studentNum,
        @Schema(defaultValue = "공과대학", description = "소속대학") String affiliation,
        @Schema(defaultValue = "12가1234", description = "차량번호") String carNum,
        @Schema(defaultValue = "true", description = "경차 여부") boolean isLight,
        @Schema(defaultValue = "010-1111-3333", description = "마스터 전화번호")
                @Phone(message = "올바른 형식의 번호를 입력하세요")
                String phoneNum,
        @Schema(defaultValue = "1", description = "선택한 구간의 id") Long selectSectorId,
        @Schema(defaultValue = "1234", description = "캡챠 코드") String captchaCode,
        @Schema(defaultValue = "1234", description = "캡챠 답변") String captchaAnswer) {

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
