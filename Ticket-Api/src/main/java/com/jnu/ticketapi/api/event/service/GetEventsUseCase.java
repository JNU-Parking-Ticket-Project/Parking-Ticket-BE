package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.response.EventPartsResponse;
import com.jnu.ticketapi.api.event.model.response.EventsPagingResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetEventsUseCase {

    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public EventsPagingResponse execute(Pageable pageable) {
        Page<Event> eventPage = eventAdaptor.findAllByOrderByIdDesc(pageable);
        return EventsPagingResponse.of(eventPage);
    }

    @Transactional(readOnly = true)
    public EventPartsResponse getParts(Long eventId) {
        return EventPartsResponse.from(eventAdaptor.findById(eventId));
    }
}
