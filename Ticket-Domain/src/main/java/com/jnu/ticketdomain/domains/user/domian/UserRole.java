package com.jnu.ticketdomain.domains.user.domian;


import lombok.Getter;

@Getter
public enum UserRole {
    /*
     ADMIN - 관리자
     USER - 일반 회원
     STUDENT - 학생
     COUNCIL - 학생회
    */
    USER("USER"),
    STUDENT("STUDENT"),
    COUNCIL("COUNCIL"),
    ADMIN("ADMIN");

    UserRole(String value) {
        this.value = value;
    }

    private final String value;
}
