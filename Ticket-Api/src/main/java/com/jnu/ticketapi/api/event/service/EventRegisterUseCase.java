package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.event.EventCreationEvent;
import com.jnu.ticketdomain.domains.events.event.EventUpdatedEvent;
import java.time.LocalDateTime;
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
        Result<Event, Object> readyEvent = eventAdaptor.findReadyOrOpenEvent();
        readyEvent.fold(
                event -> {
                    saveEventIfPresent(dateTimePeriod, event);
                    return null;
                },
                error -> {
                    firstSaveEvent(dateTimePeriod);
                    return null;
                });
    }

    private void saveEventIfPresent(DateTimePeriod dateTimePeriod, Event event) {
        event.validateIssuePeriod();
        LocalDateTime now = LocalDateTime.now();
        // 과거로 수정한 경우
        if (dateTimePeriod.getStartAt().isBefore(now)) {
            if (event.getEventStatus().equals(EventStatus.OPEN)) {
                event.updateDateTimePeriod(dateTimePeriod);
            } else {
                event.open();
                event.updateDateTimePeriod(dateTimePeriod);
            }
        } else {
            // 과거 -> 미래 로 수정한경우
            if (event.getEventStatus().equals(EventStatus.OPEN)) {
                event.ready();
                event.updateDateTimePeriod(dateTimePeriod);
            } else {
                // 미래 -> 미래 로 수정한경우
                event.updateDateTimePeriod(dateTimePeriod);
            }
            Events.raise(EventUpdatedEvent.of(event));
        }
    }

    private void firstSaveEvent(DateTimePeriod dateTimePeriod) {
        List<Sector> sectors = sectorAdaptor.findAll();
        Event event = new Event(dateTimePeriod, sectors);
        event.validateIssuePeriod();
        LocalDateTime now = LocalDateTime.now();
        // 과거로 생성한 경우
        if (now.isAfter(dateTimePeriod.getStartAt())) {
            event.open();
            Event savedEvent = eventAdaptor.save(event);
            sectors.forEach(sector -> sector.setEvent(savedEvent));
        }
        // 미래로 생성한 경우
        else {
            Event savedEvent = eventAdaptor.save(event);
            sectors.forEach(sector -> sector.setEvent(savedEvent));
            Events.raise(EventCreationEvent.of(savedEvent));
        }
    }
}
