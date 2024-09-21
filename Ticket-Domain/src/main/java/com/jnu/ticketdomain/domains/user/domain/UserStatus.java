package com.jnu.ticketdomain.domains.user.domain;


import com.jnu.ticketcommon.annotation.EnumClass;
import lombok.Getter;

@EnumClass
@Getter
public enum UserStatus {
    // 절대 이 순서를 바꾸지 마세요. 신청서를 조회할 때 합격자가 예비자보다 먼저 나오게 하기 위함입니다.
    SUCCESS("합격"),
    PREPARE("예비"),
    FAIL("불합격");

    UserStatus(String value) {
        this.value = value;
    }

    private final String value;
}
