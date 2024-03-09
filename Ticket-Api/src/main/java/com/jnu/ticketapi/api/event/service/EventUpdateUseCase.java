package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.request.EventUpdateRequest;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.event.EventUpdatedEvent;
import com.jnu.ticketdomain.domains.events.exception.CannotUpdateClosedEventException;
import com.jnu.ticketdomain.domains.events.exception.CannotUpdatePublishEventException;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class EventUpdateUseCase {
    private final EventAdaptor eventAdaptor;

    @Transactional
    public void updateEvent(EventUpdateRequest eventUpdateRequest, Long eventId) {
        DateTimePeriod.validateEventIssuePeriod(eventUpdateRequest.dateTimePeriod());
        Event event = eventAdaptor.findById(eventId);
        Validation<Seq<TicketCodeException>, Event> result = validateUpdateEvent(event);
        result.fold(
                errors -> null,
                validEvent -> {
                    Events.raise(
                            EventUpdatedEvent.of(validEvent, eventUpdateRequest.dateTimePeriod()));
                    event.update(eventUpdateRequest.title(), eventUpdateRequest.dateTimePeriod());
                    return null;
                });
    }

    Validation<Seq<TicketCodeException>, Event> validateUpdateEvent(Event event) {
        return Validation.combine(
                        validateEventStatusIsClosed(event), validateEventPublishIsTrue(event))
                .ap((eventStatus, eventPublish) -> event);
    }

    private Validation<TicketCodeException, Event> validateEventStatusIsClosed(Event event) {
        if (event.getEventStatus().equals(EventStatus.CLOSED))
            throw CannotUpdateClosedEventException.EXCEPTION;
        return Validation.valid(event);
    }

    private Validation<TicketCodeException, Event> validateEventPublishIsTrue(Event event) {
        if (Boolean.TRUE.equals(event.getPublish()))
            throw CannotUpdatePublishEventException.EXCEPTION;
        return Validation.valid(event);
    }
}
