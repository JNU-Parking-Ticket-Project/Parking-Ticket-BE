package com.jnu.ticketdomain.domains.user.domain;


import com.jnu.ticketcommon.annotation.EnumClass;
import lombok.Getter;

@EnumClass
@Getter
public enum UserRole {
    /*
    ADMIN - 관리자
    COUNCIL - 학생회
    USER - 사용자
    */
    ADMIN("ADMIN"),
    COUNCIL("COUNCIL"),
    USER("USER");

    UserRole(String value) {
        this.value = value;
    }

    private final String value;
}
