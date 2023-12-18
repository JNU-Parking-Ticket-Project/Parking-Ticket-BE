package com.jnu.ticketbatch;


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

    @SneakyThrows
    @Async
    @TransactionalEventListener(
            classes = EventUpdatedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventUpdatedEvent eventCreationEvent) {
        // 예약 스케줄링
        Event event = eventCreationEvent.getEvent();
        eventUpdateJob.reRegisterJob(event.getId(), event.getDateTimePeriod().getStartAt());
    }
}
