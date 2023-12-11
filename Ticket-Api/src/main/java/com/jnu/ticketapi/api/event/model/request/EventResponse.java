package com.jnu.ticketapi.api.event.model.request;


import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventResponse {
    private Long eventId;
    private EventStatus status;

    public static EventResponse of(Event event) {
        return EventResponse.builder()
                .eventId(event.getId())
                .status(event.getEventStatus())
                .build();
    }
}
