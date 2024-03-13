package com.jnu.ticketbatch;


import com.jnu.ticketbatch.job.EventRegisterJob;
import com.jnu.ticketbatch.job.EventUpdateJob;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.event.EventDeletedEvent;
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
public class EventDeletedEventHandler {
    private final EventRegisterJob eventRegisterJob;
    private final EventUpdateJob eventUpdateJob;

    @SneakyThrows
    @Async
    @TransactionalEventListener(
            classes = EventDeletedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventDeletedEvent eventDeletedEvent) {
        // 예약 스케줄링
        Event event = eventDeletedEvent.getEvent();
        try {
            eventUpdateJob.cancelScheduledJob(event.getId());
        } catch (Exception e) {
            log.info("스케줄링 실패 : " + e.getMessage());
            throw
        }
    }
}
