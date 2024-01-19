package com.jnu.ticketapi.api.event.model.response;


import com.jnu.ticketapi.api.sector.model.response.SectorReadResponse;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.domain.Event;
import java.util.List;
import lombok.Builder;

@Builder
public record EventDetailResponse(
        List<SectorReadResponse> sectors,
        String eventTitle,
        String eventStatus,
        DateTimePeriod dateTimePeriod) {

    public static EventDetailResponse from(List<SectorReadResponse> sectors, Event event) {
        return EventDetailResponse.builder()
                .sectors(sectors)
                .eventTitle(event.getTitle())
                .eventStatus(event.getEventStatus().name())
                .dateTimePeriod(event.getDateTimePeriod())
                .build();
    }
}
