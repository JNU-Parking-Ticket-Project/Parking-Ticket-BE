package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.domains.events.domain.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventDeletionEvent extends DomainEvent {
    private final String couponCode;
    private final Long eventId;

    public static EventDeletionEvent of(Event event) {
        return EventDeletionEvent.builder()
                .couponCode(event.getCouponCode())
                .eventId(event.getId())
                .build();
    }
}
