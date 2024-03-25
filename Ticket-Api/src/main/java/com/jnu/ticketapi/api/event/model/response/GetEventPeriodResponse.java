package com.jnu.ticketapi.api.event.model.response;


import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import lombok.Builder;

@Builder
public record GetEventPeriodResponse(DateTimePeriod dateTimePeriod, Long eventId) {

    public static GetEventPeriodResponse of(DateTimePeriod dateTimePeriod, Long eventId) {
        return GetEventPeriodResponse.builder()
                .dateTimePeriod(dateTimePeriod)
                .eventId(eventId)
                .build();
    }
}
