package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketapi.api.event.model.response.GetEventPeriodResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.consts.TicketStatic;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.*;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketinfrastructure.config.redis.redissonLock.RedissonLock;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class EventWithDrawUseCase {

    private final WaitingQueueService waitingQueueService;
    private final EventAdaptor eventAdaptor;

    /** 재고 감소 */
    @RedissonLock(
            LockName = "주차권_발급",
            identifier = "userId",
            waitTime = 5000,
            leaseTime = 10000,
            timeUnit = TimeUnit.MILLISECONDS)
    @SneakyThrows
    public void issueEvent(Registration registration, Long userId, Long sectorId, Long eventId) {
        // 재고 감소 로직 구현
        Result<Event, Object> readyEvent = eventAdaptor.findReadyOrOpenEvent();
        readyEvent.fold(
                (event) -> {
                    if (event.getEventStatus().equals(EventStatus.READY))
                        throw NotOpenEventStatusException.EXCEPTION;
                    event.validateIssuePeriod();
                    return null;
                },
                (error) -> {
                    throw NotReadyEventStatusException.EXCEPTION;
                });
        waitingQueueService.registerQueue(REDIS_EVENT_ISSUE_STORE, registration, userId, sectorId, eventId);
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
