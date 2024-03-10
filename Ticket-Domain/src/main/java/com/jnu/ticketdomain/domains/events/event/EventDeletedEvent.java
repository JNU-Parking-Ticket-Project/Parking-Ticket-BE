package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.domains.events.domain.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventDeletedEvent extends DomainEvent {
    private final Event event;

    public static EventDeletedEvent of(Event event) {
        return EventDeletedEvent.builder().event(event).build();
    }
}
