package com.jnu.ticketapi.api.event.model.request;


import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import lombok.Builder;

public record EventRegisterRequest(DateTimePeriod dateTimePeriod, String title) {
    @Builder
    public EventRegisterRequest {}
}
