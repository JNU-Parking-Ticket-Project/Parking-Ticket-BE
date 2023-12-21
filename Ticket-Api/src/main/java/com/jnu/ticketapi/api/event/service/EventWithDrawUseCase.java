package com.jnu.ticketapi.api.event.service;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.common.aop.redissonLock.RedissonLock;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketdomain.domains.events.exception.NotReadyEventStatusException;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class EventWithDrawUseCase {

    private final WaitingQueueService waitingQueueService;
    private final EventAdaptor eventAdaptor;

    /** 재고 감소 */
    @Transactional
    @RedissonLock(
            LockName = "주차권_발급",
            waitTime = 3000,
            leaseTime = 3000,
            timeUnit = TimeUnit.MILLISECONDS)
    public void issueEvent(Long userId) {
        // 재고 감소 로직 구현
        Event openEvent = eventAdaptor.findOpenEvent();
        // openEvent.validateIssuePeriod();
        waitingQueueService.registerQueue(REDIS_EVENT_ISSUE_STORE, userId);
    }

    @Transactional(readOnly = true)
    public Long getEventOrder() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return waitingQueueService.getWaitingOrder(REDIS_EVENT_ISSUE_STORE, currentUserId);
    }

    public DateTimePeriod getEventPeriod() {
        Result<Event, Object> readyEvent = eventAdaptor.findReadyEvent();
        return readyEvent.fold(
                (event) -> {
                    return event.getDateTimePeriod();
                },
                (error) -> {
                    throw NotReadyEventStatusException.EXCEPTION;
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
}
