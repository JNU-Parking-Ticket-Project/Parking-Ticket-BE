package com.jnu.ticketbatch;


import com.jnu.ticketbatch.job.EventRegisterJob;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.event.EventBeforeCreationEvent;
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
public class EventBeforeCreationEventHandler {
    private final EventRegisterJob eventRegisterJob;

    @SneakyThrows
    @Async
    @TransactionalEventListener(
            classes = EventBeforeCreationEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventBeforeCreationEvent eventCreationEvent) {
        Event event = eventCreationEvent.getEvent();
        eventRegisterJob.expiredJob(event.getId(), event.getDateTimePeriod().getEndAt());
    }
}
