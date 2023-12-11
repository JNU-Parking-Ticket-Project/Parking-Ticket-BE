package com.jnu.ticketdomain.domains.event.domain;


import com.jnu.ticketcommon.annotation.EnumClass;

@EnumClass
public enum CouponStatus {
    READY("READY", "발행준비중"),
    OPEN("OPEN", "발행중"),
    EXPIRED("EXPIRED", "만료");

    private final String name;
    private final String value;

    CouponStatus(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
