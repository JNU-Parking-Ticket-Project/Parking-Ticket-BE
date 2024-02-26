package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.request.EventRegisterRequest;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.event.EventCreationEvent;
import com.jnu.ticketdomain.domains.events.event.EventUpdatedEvent;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class EventRegisterUseCase {
    private final EventAdaptor eventAdaptor;

    @Transactional
    public void registerEvent(EventRegisterRequest eventRegisterRequest) {
        firstSaveEvent(eventRegisterRequest);
    }

    /**
     * 이벤트의 PUBLISH 상태가 TRUE인 경우는 수정이 불가능하다. 기존 이벤트가 존재한 경우 단, (과거, 과거)는 수정이 불가하다. (과거, 미래) -> (과거,
     * 미래 or 과거)로 수정한 경우 (과거, 미래) -> (과거, 과거)로 수정한 경우
     */
    private void saveEventIfPresent(EventRegisterRequest eventRegisterRequest, Event event) {
        event.validationPublishStatus();
        event.validateIssuePeriod();
        LocalDateTime now = LocalDateTime.now();
        Option.of(eventRegisterRequest.dateTimePeriod().getStartAt().isBefore(now))
                .peek(
                        b -> {
                            // (과거, 미래) -> (미래, 미래)로 수정한 경우
                            if (eventRegisterRequest.dateTimePeriod().getStartAt().isAfter(now)
                                    && eventRegisterRequest
                                    .dateTimePeriod()
                                    .getEndAt()
                                    .isAfter(now)) {
                                event.ready();
                            } else {
                                // (과거, 미래) -> 과거, 미래)로 수정한 경우
                                if (eventRegisterRequest.dateTimePeriod().getEndAt().isAfter(now)) {
                                    event.open();
                                } else {
                                    // (과거, 미래) -> (과거, 과거)로 수정한 경우
                                    event.close();
                                }
                            }
                        });
        // (미래, 미래) -> (과거, 미래) 로 수정한경우 or (미래, 미래) -> (미래, 미래)로 수정한 경우 , (미래, 미래) -> (과거, 과거)로 수정한
        // 경우
        Option.of(event.getDateTimePeriod().getStartAt().isAfter(now))
                .filter(b -> eventRegisterRequest.dateTimePeriod().getStartAt().isBefore(now))
                .peek(
                        b -> {
                            // (미래, 미래) -> (과거, 과거)로 수정한 경우
                            if (eventRegisterRequest.dateTimePeriod().getEndAt().isBefore(now)) {
                                event.close();
                            } else {
                                // (미래, 미래) -> (과거, 미래)로 수정한 경우
                                Events.raise(EventUpdatedEvent.of(event));
                                event.open();
                            }
                        })
                // (미래, 미래) -> (미래, 미래)로 수정한 경우
                .onEmpty(
                        () -> {
                            Events.raise(EventUpdatedEvent.of(event));
                            event.ready();
                        });
        event.updateDateTimePeriod(eventRegisterRequest.dateTimePeriod());
    }

    /**
     * 기존 이벤트가 존재하지 않는 경우 단, (과거, 과거)는 생성이 불가하다. (과거, 미래), (미래, 미래)로 생성한 경우
     */
    private void firstSaveEvent(EventRegisterRequest eventRegisterRequest) {
        //        List<Sector> sectors = sectorAdaptor.findAll();
        Event event =
                new Event(
                        eventRegisterRequest.dateTimePeriod(), null, eventRegisterRequest.title());
        event.validateIssuePeriod();
        LocalDateTime now = LocalDateTime.now();
        Option.of(eventRegisterRequest.dateTimePeriod().getStartAt().isBefore(now))
                .filter(b -> now.isAfter(eventRegisterRequest.dateTimePeriod().getStartAt()))
                .peek(
                        b -> {
                            event.open();
                            Event savedEvent = eventAdaptor.save(event);
                            //                            sectors.forEach(sector ->
                            // sector.setEvent(savedEvent));
                        })
                .onEmpty(
                        () -> {
                            Event savedEvent = eventAdaptor.save(event);
                            //                            sectors.forEach(sector ->
                            // sector.setEvent(savedEvent));
                            // 이벤트 상태 변경 Batch 스케줄링 (READY -> OPEN) - 이벤트
                            Events.raise(EventCreationEvent.of(savedEvent));
                        });
    }
}
