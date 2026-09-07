package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.request.EventResponse;
import com.jnu.ticketapi.api.event.model.request.UpdateEventStatusRequest;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.RedisStockUnavailableException;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateEventStatusUseCase {
    private static final Duration REDIS_STOCK_DRAIN_TIMEOUT = Duration.ofMinutes(5);

    private final EventAdaptor eventAdaptor;
    private final SectorAdaptor sectorAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Transactional
    //    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    public EventResponse execute(Long eventId, UpdateEventStatusRequest updateEventStatusRequest) {
        final Event event = eventAdaptor.findByIdForUpdate(eventId);
        final EventStatus status = updateEventStatusRequest.getStatus();
        if (status == EventStatus.OPEN) {
            event.validateReadyToOpen();
            initializeAdmission(eventId);
        }
        if (status == EventStatus.CLOSED && waitingQueueService != null) {
            closeEventStock(eventId);
        }
        return EventResponse.of(eventAdaptor.updateEventStatus(event, status));
    }

    private void closeEventStock(Long eventId) {
        try {
            waitingQueueService.markEventStockClosed(eventId, REDIS_STOCK_DRAIN_TIMEOUT);
            waitingQueueService.expireEventStockKeys(eventId, REDIS_STOCK_DRAIN_TIMEOUT);
        } catch (DataAccessException exception) {
            log.warn(
                    "Redis event stock could not be closed; DB event status remains authoritative."
                            + " eventId: {}",
                    eventId,
                    exception);
        }
    }

    private void initializeAdmission(Long eventId) {
        if (waitingQueueService == null) {
            throw RedisStockUnavailableException.EXCEPTION;
        }
        try {
            boolean initialized =
                    waitingQueueService.initializeEventStock(
                            eventId, sectorAdaptor.findByEventId(eventId));
            if (!initialized) {
                throw RedisStockUnavailableException.EXCEPTION;
            }
        } catch (DataAccessException exception) {
            throw RedisStockUnavailableException.EXCEPTION;
        }
    }
}
