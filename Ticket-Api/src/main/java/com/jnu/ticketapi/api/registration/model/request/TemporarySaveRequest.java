package com.jnu.ticketapi.api.registration.model.request;


import com.jnu.ticketcommon.annotation.Phone;
import com.jnu.ticketcommon.message.ValidationMessage;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.*;
import lombok.Builder;

@Builder
public record TemporarySaveRequest(
        @Schema(defaultValue = "홍길동", description = "이름")
                @NotBlank(message = "이름을 " + ValidationMessage.MUST_NOT_BLANK)
                @Size(min = 1, max = 100, message = ValidationMessage.MUST_NOT_OVER_100)
                String name,
        @Schema(defaultValue = "183027", description = "학번")
                @NotBlank(message = "학번을 " + ValidationMessage.MUST_NOT_BLANK)
                String studentNum,
        @Schema(defaultValue = "공과대학", description = "소속대학")
                @NotBlank(message = "소속대학을 " + ValidationMessage.MUST_NOT_BLANK)
                String affiliation,
        @Schema(defaultValue = "컴퓨터정보통신공학과", description = "소속학과")
            @NotBlank(message = "소속학과를 " + ValidationMessage.MUST_NOT_BLANK)
            String department,
        @Schema(defaultValue = "12가1234", description = "차량번호")
                @NotBlank(message = "차량번호를 " + ValidationMessage.MUST_NOT_BLANK)
                String carNum,
        @Schema(defaultValue = "true", description = "경차 여부")
                @NotNull(message = "경차 여부를 " + ValidationMessage.MUST_NOT_NULL)
                Boolean isLight,
        @Schema(defaultValue = "010-1111-3333", description = "마스터 전화번호")
                @Phone(message = ValidationMessage.IS_NOT_VALID_PHONE)
                String phoneNum,
        @Schema(defaultValue = "1", description = "선택한 구간의 id")
                @Positive(message = "구간 ID는 " + ValidationMessage.MUST_POSITIVE_NUMBER)
                Long selectSectorId) {

    ///
    public Registration toEntity(
            TemporarySaveRequest requestDto, Sector sector, String email, User user) {
        return Registration.builder()
                .email(email)
                .name(requestDto.name())
                .studentNum(requestDto.studentNum())
                .affiliation(requestDto.affiliation())
                .department(requestDto.department())
                .carNum(requestDto.carNum())
                .isLight(requestDto.isLight())
                .phoneNum(requestDto.phoneNum())
                .sector(sector)
                .isSaved(false)
                .user(user)
                .build();
    }
}
