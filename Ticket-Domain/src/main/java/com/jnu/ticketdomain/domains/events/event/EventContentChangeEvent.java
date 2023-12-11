package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.domains.events.domain.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventContentChangeEvent extends DomainEvent {
    private final String couponCode;
    private final Long eventId;

    public static EventContentChangeEvent of(Event event) {
        return EventContentChangeEvent.builder()
                .couponCode(event.getCouponCode())
                .eventId(event.getId())
                .build();
    }
}
