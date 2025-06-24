package com.jnu.ticketdomain.domains.user.domain;


import com.jnu.ticketcommon.annotation.EnumClass;
import lombok.Getter;

@EnumClass
@Getter
public enum UserStatus {
    SUCCESS("합격"),
    PREPARE("예비"),
    FAIL("불합격");

    UserStatus(String value) {
        this.value = value;
    }

    private final String value;
}
