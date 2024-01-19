package com.jnu.ticketapi.api.event.model.response;


import com.jnu.ticketdomain.domains.events.domain.Event;
import lombok.Builder;

public record EventPartsResponse(String eventTitle, String eventStatus) {

    @Builder
    public EventPartsResponse {}

    public static EventPartsResponse from(Event event) {
        return EventPartsResponse.builder()
                .eventTitle(event.getTitle())
                .eventStatus(event.getEventStatus().toString())
                .build();
    }
}
