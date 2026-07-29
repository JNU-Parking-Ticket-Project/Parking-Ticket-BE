package com.jnu.ticketbatch;


import com.jnu.ticketbatch.job.EventRegisterJob;
import com.jnu.ticketbatch.job.EventUpdateJob;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.event.EventUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventUpdatedEventHandler {

    private final EventUpdateJob eventUpdateJob;
    private final EventRegisterJob eventRegisterJob;

    @SneakyThrows
    @Async
    @TransactionalEventListener(
            classes = EventUpdatedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventUpdatedEvent eventUpdateEvent) {
        Event event = eventUpdateEvent.getCurrentEvent();
        try {
            eventUpdateJob.cancelScheduledJob(event.getId());
            // 새로운 EXPIREDJOB 등록
            eventRegisterJob.registerJob(
                    event.getId(), eventUpdateEvent.getDateTimePeriod().getStartAt());
            eventRegisterJob.expiredJob(
                    event.getId(), eventUpdateEvent.getDateTimePeriod().getEndAt());
        } catch (Exception e) {
            log.info("스케줄링 실패 : " + e.getMessage());
            throw new RuntimeException("스케줄링 실패 : " + e.getMessage());
        }
    }
}
