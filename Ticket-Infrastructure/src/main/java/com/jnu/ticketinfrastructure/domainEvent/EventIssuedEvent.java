package com.jnu.ticketinfrastructure.domainEvent;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventIssuedEvent extends InfrastructureEvent {
    private final Long currentUserId;

    public static EventIssuedEvent from(Long currentUserId) {
        return EventIssuedEvent.builder().currentUserId(currentUserId).build();
    }
}
