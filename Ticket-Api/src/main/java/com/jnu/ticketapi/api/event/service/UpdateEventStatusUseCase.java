package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.request.EventResponse;
import com.jnu.ticketapi.api.event.model.request.UpdateEventStatusRequest;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateEventStatusUseCase {
    private final EventAdaptor eventAdaptor;
    private final SectorAdaptor sectorAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Transactional
    //    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    public EventResponse execute(Long eventId, UpdateEventStatusRequest updateEventStatusRequest) {
        final Event event = eventAdaptor.findById(eventId);
        final EventStatus status = updateEventStatusRequest.getStatus();
        if (status == EventStatus.OPEN && waitingQueueService != null) {
            waitingQueueService.initializeEventStock(
                    eventId, sectorAdaptor.findByEventId(eventId));
        }
        return EventResponse.of(eventAdaptor.updateEventStatus(event, status));
    }
}
