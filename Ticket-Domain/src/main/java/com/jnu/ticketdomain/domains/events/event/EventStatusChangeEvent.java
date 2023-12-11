package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.domains.events.domain.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventStatusChangeEvent extends DomainEvent {
    private final String eventCode;
    private final Long eventId;

    public static EventStatusChangeEvent of(Event event) {
        return EventStatusChangeEvent.builder()
                .eventCode(event.getEventCode())
                .eventId(event.getId())
                .build();
    }
}
