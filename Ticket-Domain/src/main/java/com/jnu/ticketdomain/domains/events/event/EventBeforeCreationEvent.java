package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.domains.events.domain.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventBeforeCreationEvent extends DomainEvent {
    private final Event event;

    public static EventBeforeCreationEvent of(Event event) {
        return EventBeforeCreationEvent.builder().event(event).build();
    }
}
