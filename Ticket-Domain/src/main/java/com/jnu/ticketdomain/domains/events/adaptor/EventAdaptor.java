package com.jnu.ticketdomain.domains.events.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketdomain.domains.events.exception.UseOtherApiException;
import com.jnu.ticketdomain.domains.events.out.EventLoadPort;
import com.jnu.ticketdomain.domains.events.out.EventRecordPort;
import com.jnu.ticketdomain.domains.events.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Adaptor
@RequiredArgsConstructor
public class EventAdaptor implements EventRecordPort, EventLoadPort {
    private final EventRepository eventRepository;

    @Override
    public Event findById(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> NotFoundEventException.EXCEPTION);
    }

    @Override
    public Event findOpenEvent() {
        return eventRepository
                // TODO 추 후에 완성되면 이걸로 변경         .findByCouponStatus(CouponStatus.OPEN)
                .findByEventStatus(EventStatus.OPEN)
                .orElseThrow(() -> NotFoundEventException.EXCEPTION);
    }

    @Override
    public Result<Event, Object> findReadyOrOpenEvent() {
        // READY 상태의 이벤트가 없으면 OPEN 상태의 이벤트를 가져온다.
        Optional<Event> event = eventRepository.findByEventStatus(EventStatus.READY);
        if (event.isPresent()) return Result.success(event.get());
        event = eventRepository.findByEventStatus(EventStatus.OPEN);
        return event.map(Result::success)
                .orElseGet(() -> Result.failure(NotFoundEventException.EXCEPTION));
    }

    @Override
    public Result<Event, Object> findReadyOrOpenAndNotPublishEvent() {
        // READY 상태의 이벤트가 없으면 OPEN 상태의 이벤트를 가져온다.
        Optional<Event> event = eventRepository.findByEventStatus(EventStatus.READY);
        if (event.isPresent() & !event.get().getPublish()) return Result.success(event.get());
        event = eventRepository.findByEventStatus(EventStatus.OPEN);
        return event.filter(e -> !e.getPublish())
                .map(Result::success)
                .orElseGet(() -> Result.failure(NotFoundEventException.EXCEPTION));
    }

    @Override
    public Event findRecentEvent() {
        Event event =
                findReadyOrOpenAndNotPublishEvent()
                        .fold(
                                Result::success,
                                error -> {
                                    Pageable pageRequest = PageRequest.of(0, 1);
                                    Event event1 =
                                            eventRepository
                                                    .findClosestClosedEvent(
                                                            LocalDateTime.now(), pageRequest)
                                                    .get(0);
                                    return Result.success(event1);
                                })
                        .getOrThrow();

        if (event == null) {
            throw NotFoundEventException.EXCEPTION;
        }
        return event;
    }

    @Override
    public Result<Event, Object> findReadyEvent() {
        Optional<Event> event = eventRepository.findByEventStatus(EventStatus.READY);
        return event.map(Result::success)
                .orElseGet(() -> Result.failure(NotFoundEventException.EXCEPTION));
    }

    public Event updateEventStatus(Event event, EventStatus status) {
        if (status == EventStatus.CLOSED) event.close();
        else if (status == EventStatus.OPEN) event.open();
        else if (status == EventStatus.CALCULATING) event.calculate();
        else if (status == EventStatus.READY) event.ready();
        else throw UseOtherApiException.EXCEPTION; // open, deleteSoft 는 다른 API 강제
        return eventRepository.save(event);
    }

    public List<Event> findEventsByEndAtBeforeAndStatusOpen(LocalDateTime time) {
        return eventRepository.findByEndAtBeforeAndStatusOpen(time);
    }

    public List<Event> closeExpiredEventsEndAtBeforeOpen(LocalDateTime time) {
        List<Event> events = findEventsByEndAtBeforeAndStatusOpen(time);
        events.forEach(event -> updateEventStatus(event, EventStatus.CLOSED));
        eventRepository.saveAll(events);
        return events;
    }

    @Override
    public Event save(Event event) {
        return eventRepository.save(event);
    }

    @Override
    public Page<Event> findAllByOrderByIdDesc(Pageable pageable) {
        return eventRepository.findAllByOrderByIdDesc(pageable);
    }
}
