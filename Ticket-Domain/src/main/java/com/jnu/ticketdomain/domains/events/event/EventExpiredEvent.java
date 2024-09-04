package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventExpiredEvent extends DomainEvent {
    private final Long eventId;

    public EventExpiredEvent(Long eventId) {
        this.eventId = eventId;
    }
}