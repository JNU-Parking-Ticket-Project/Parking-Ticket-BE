package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.request.EventRegisterRequest;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.service.RegistrationUseCase;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class TestUseCase {
    private final EventAdaptor eventAdaptor;
    private final EventRepository eventRepository;
    private final EventRegisterUseCase eventRegisterUseCase;
    private final RegistrationUseCase registrationUseCase;
    private final EventDeleteUseCase eventDeleteUseCase;
    private final AtomicInteger counter = new AtomicInteger(0);
    private static final Integer TEST_SCHEDULER_START_TIME_AFTER_NOW = 1;
    private static final Integer TEST_SCHEDULER_END_TIME_AFTER_NOW = 1000;

    @Transactional
    public void execute() {
        Optional<Event> event =
                eventRepository.findAll().stream().max(Comparator.comparing(Event::getId));
        event.ifPresent(e -> eventDeleteUseCase.deleteEvent(e.getId()));

        DateTimePeriod dateTimePeriod =
                new DateTimePeriod(
                        LocalDateTime.now().plusSeconds(TEST_SCHEDULER_START_TIME_AFTER_NOW),
                        LocalDateTime.now().plusMinutes(TEST_SCHEDULER_END_TIME_AFTER_NOW));

        EventRegisterRequest eventRegisterRequest =
                new EventRegisterRequest(dateTimePeriod, "test");
        eventRegisterUseCase.registerEvent(eventRegisterRequest);
    }

    @Transactional
    public void execute2() {
        Event event = eventAdaptor.findOpenEvent();
        Sector sector = new Sector("1구간", "사회대", 20, 40);
        Sector sector2 = new Sector("2구간", "사회대2", 30, 30);
        Sector sector3 = new Sector("3구간", "사회대3", 40, 20);
        Sector sector4 = new Sector("4구간", "사회대4", 5, 55);
        Sector sector5 = new Sector("5구간", "사회대5", 10, 50);
        sector.setEvent(event);
        sector2.setEvent(event);
        sector3.setEvent(event);
        sector4.setEvent(event);
        sector5.setEvent(event);
        List<Sector> sectorList = new ArrayList<>();
        sectorList.add(sector);
        sectorList.add(sector2);
        sectorList.add(sector3);
        sectorList.add(sector4);
        sectorList.add(sector5);
        event.setSector(sectorList);
        event.isPublish(true);
    }

    @Transactional
    public void execute3() {
        String email1 = "ekrrdj07@naver.com";
        String email2 = "ekrrdj07@gmail.com";
        Event event = eventAdaptor.findOpenEvent();
        FinalSaveRequest request =
                FinalSaveRequest.builder()
                        .name("이진혁")
                        .studentNum(String.valueOf(counter.get()))
                        .affiliation("컴퓨터공학과")
                        .carNum("12가1234")
                        .isLight(true)
                        .phoneNum("010-1111-3333")
                        .selectSectorId(event.getSector().get(0).getId())
                        .captchaCode("uTz0clvmTKvmU/6JmOSXng==")
                        .captchaAnswer("1234")
                        .build();
        registrationUseCase.finalSave(request, email1, event.getId());
        registrationUseCase.finalSave(request, email2, event.getId());
        increment();
    }

    private void increment() {
        counter.incrementAndGet();
    }
}
