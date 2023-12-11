package com.jnu.ticketdomain.domains.events.domain;


import com.jnu.ticketcommon.annotation.EnumClass;

@EnumClass
public enum EventStatus {
    READY("READY", "발행준비중"),
    OPEN("OPEN", "발행중"),
    CALCULATING("CALCULATING", "정산중"),
    CLOSED("EXPIRED", "만료");

    private final String name;
    private final String value;

    EventStatus(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
