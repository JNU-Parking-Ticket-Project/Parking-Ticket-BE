package com.jnu.ticketdomain.domains.events.event;


import com.jnu.ticketdomain.common.domainEvent.DomainEvent;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.domain.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventUpdatedEvent extends DomainEvent {
    private final Event currentEvent;
    private final DateTimePeriod dateTimePeriod;

    public static EventUpdatedEvent of(Event event, DateTimePeriod dateTimePeriod) {
        return EventUpdatedEvent.builder()
                .currentEvent(event)
                .dateTimePeriod(dateTimePeriod)
                .build();
    }
}
