package com.jnu.ticketinfrastructure.domainEvent;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventIssuedEvent extends InfrastructureEvent {
    private final Long currentUserId;
    private final Long eventId;

    public static EventIssuedEvent from(Long currentUserId, Long eventId) {
        return EventIssuedEvent.builder().currentUserId(currentUserId).eventId(eventId).build();
    }
}
