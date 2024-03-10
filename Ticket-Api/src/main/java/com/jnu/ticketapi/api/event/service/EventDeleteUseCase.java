package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.event.EventDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class EventDeleteUseCase {
    private final EventAdaptor eventAdaptor;

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        Events.raise(EventDeletedEvent.of(event));
        event.deleteEvent();
    }
}
