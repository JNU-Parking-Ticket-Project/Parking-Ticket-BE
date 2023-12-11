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

@Adaptor
@RequiredArgsConstructor
public class EventAdaptor implements EventRecordPort, EventLoadPort {
    private final EventRepository eventRepository;

    public Event findById(Long couponId) {
        return eventRepository
                .findById(couponId)
                .orElseThrow(() -> NotFoundEventException.EXCEPTION);
    }

    public Event findOpenEvent() {
        return eventRepository
                // TODO 추 후에 완성되면 이걸로 변경         .findByCouponStatus(CouponStatus.OPEN)
                .findByEventStatus(EventStatus.OPEN)
                .orElseThrow(() -> NotFoundEventException.EXCEPTION);
    }

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
}
