package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
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
public class EventRegisterUseCase {
    private final EventAdaptor eventAdaptor;
    private final SectorAdaptor sectorAdaptor;

    @Transactional
    public void registerEvent(DateTimePeriod dateTimePeriod) {
        List<Sector> sectors = sectorAdaptor.findAll();
        Event event = new Event(dateTimePeriod, sectors);
        event.validateIssuePeriod();
        Event savedEvent = eventAdaptor.save(event);
        // TODO  기존에 Open 상태의 이벤트가 있으면 Close 시킨다.
        sectors.forEach(sector -> sector.setEvent(savedEvent));
    }
}
