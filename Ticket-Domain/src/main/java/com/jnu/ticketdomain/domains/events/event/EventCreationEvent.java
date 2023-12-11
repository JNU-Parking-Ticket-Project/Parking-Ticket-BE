package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventCreationEvent extends DomainEvent {
    private final String couponCode;
    private final Long eventId;

    public static EventCreationEvent of(String couponCode, Long eventId) {
        return EventCreationEvent.builder().couponCode(couponCode).eventId(eventId).build();
    }
}
