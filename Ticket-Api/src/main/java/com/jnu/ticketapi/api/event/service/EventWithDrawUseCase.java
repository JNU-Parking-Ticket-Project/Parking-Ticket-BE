package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.response.GetEventPeriodResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.*;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class EventWithDrawUseCase {

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    private final EventAdaptor eventAdaptor;

    /** 재고 감소 */
    //    @RedissonLock(
    //            LockName = "주차권_발급",
    //            identifier = "userId",
    //            waitTime = 5000,
    //            leaseTime = 10000,
    //            timeUnit = TimeUnit.MILLISECONDS)
    @SneakyThrows
    public StockReservationResult issueEvent(
            Registration registration, Long userId, Sector sector, Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        if (event.getEventStatus() != EventStatus.OPEN) {
            throw NotOpenEventStatusException.EXCEPTION;
        }
        if (sector.getEvent() == null || !Objects.equals(sector.getEvent().getId(), eventId)) {
            throw NotFoundSectorException.EXCEPTION;
        }
        event.validateIssuePeriod();

        StockReservationResult result =
                waitingQueueService.reserveAndRegisterQueue(
                        waitingQueueService.eventStreamKey(eventId),
                        registration,
                        userId,
                        sector,
                        eventId);
        if (result.isDuplicate()) {
            throw AlreadyExistRegistrationException.EXCEPTION;
        }
        if (result.isNoStock()) {
            throw NoEventStockLeftException.EXCEPTION;
        }
        return result;
    }

    public GetEventPeriodResponse getEventPeriod() {
        Result<Event, Object> readyEvent = eventAdaptor.findReadyOrOpenEvent();
        return readyEvent.fold(
                (event) -> {
                    if (event.getPublish().equals(false)) throw NotPublishEventException.EXCEPTION;
                    return GetEventPeriodResponse.of(event.getDateTimePeriod(), event.getId());
                },
                (error) -> {
                    throw AlreadyCloseStatusException.EXCEPTION;
                });
    }

    public void resetEvent() {
        Result<Event, Object> readyOrOpenEvent = eventAdaptor.findReadyOrOpenEvent();
        readyOrOpenEvent.fold(
                (event) -> {
                    eventAdaptor.updateEventStatus(event, EventStatus.CLOSED);
                    List<Sector> sector = event.getSector();
                    sector.forEach(Sector::resetAmount);
                    if (waitingQueueService != null) {
                        waitingQueueService.deleteEventStockKeys(event.getId());
                    }
                    return null;
                },
                (error) -> {
                    throw NotFoundEventException.EXCEPTION;
                });
    }

    @Transactional(readOnly = true)
    public DateTimePeriod getEventPeriodByEventId(Long eventId) {
        return eventAdaptor.findById(eventId).getDateTimePeriod();
    }
}
