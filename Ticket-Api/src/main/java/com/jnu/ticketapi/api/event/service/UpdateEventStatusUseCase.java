package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.request.EventResponse;
import com.jnu.ticketapi.api.event.model.request.UpdateEventStatusRequest;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateEventStatusUseCase {
    private static final Duration REDIS_STOCK_DRAIN_TIMEOUT = Duration.ofMinutes(5);

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
        if (status == EventStatus.CLOSED && waitingQueueService != null) {
            closeEventStock(eventId);
        }
        return EventResponse.of(eventAdaptor.updateEventStatus(event, status));
    }

    private void closeEventStock(Long eventId) {
        waitingQueueService.markEventStockClosed(eventId, REDIS_STOCK_DRAIN_TIMEOUT);
        for (Sector sector : sectorAdaptor.findByEventId(eventId)) {
            waitingQueueService
                    .findRemainingStock(eventId, sector.getId())
                    .ifPresent(
                            remainingAmount -> {
                                sector.syncRemainingAmount(remainingAmount);
                                sectorAdaptor.save(sector);
                            });
        }
        waitingQueueService.expireEventStockKeys(eventId, REDIS_STOCK_DRAIN_TIMEOUT);
    }
}
