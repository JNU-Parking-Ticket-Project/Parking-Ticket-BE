package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class OpenEventUseCase {
    private final EventAdaptor eventAdaptor;

    @Transactional
    public void execute() {
        Result<Event, Object> readyEvent = eventAdaptor.findReadyEvent();
        readyEvent.fold(
                event -> {
                    eventAdaptor.updateEventStatus(event, EventStatus.OPEN);
                    return null;
                },
                event -> {
                    throw NotFoundEventException.EXCEPTION;
                });
    }
}
