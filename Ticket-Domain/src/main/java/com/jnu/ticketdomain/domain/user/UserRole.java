package com.jnu.ticketdomain.domain.user;


import lombok.Getter;

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
