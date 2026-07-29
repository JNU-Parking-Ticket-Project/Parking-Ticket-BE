package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class OpenEventUseCase {
    private final EventAdaptor eventAdaptor;
    private final SectorAdaptor sectorAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Transactional
    public void execute() {
        Result<Event, Object> readyEvent = eventAdaptor.findReadyEvent();
        readyEvent.fold(
                event -> {
                    if (waitingQueueService != null) {
                        waitingQueueService.initializeEventStock(
                                event.getId(), sectorAdaptor.findByEventId(event.getId()));
                    }
                    eventAdaptor.updateEventStatus(event, EventStatus.OPEN);
                    return null;
                },
                event -> {
                    throw NotFoundEventException.EXCEPTION;
                });
    }
}
