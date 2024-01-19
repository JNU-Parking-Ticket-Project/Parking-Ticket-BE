package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.response.EventDetailResponse;
import com.jnu.ticketapi.api.sector.model.response.SectorReadResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetEventDetailUseCase {

    private final EventAdaptor eventAdaptor;
    private final SectorAdaptor sectorAdaptor;

    @Transactional(readOnly = true)
    public EventDetailResponse execute(Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        List<Sector> sectors = sectorAdaptor.findByEventId(eventId);
        return EventDetailResponse.from(SectorReadResponse.toSectorReadResponses(sectors), event);
    }
}
