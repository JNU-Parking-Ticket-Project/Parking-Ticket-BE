package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class OpenEventUseCase {
    private final EventAdaptor eventAdaptor;

    @Transactional
    public Event execute() {
        Event event = eventAdaptor.findReadyEvent();
        return eventAdaptor.updateEventStatus(event, EventStatus.OPEN);
    }
}
