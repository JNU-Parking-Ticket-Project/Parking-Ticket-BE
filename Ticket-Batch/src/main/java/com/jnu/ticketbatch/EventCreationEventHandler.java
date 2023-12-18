package com.jnu.ticketbatch;


import com.jnu.ticketbatch.job.EventRegisterJob;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.event.EventCreationEvent;
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
public class EventCreationEventHandler {
    private final EventRegisterJob eventRegisterJob;

    @SneakyThrows
    @Async
    @TransactionalEventListener(
            classes = EventCreationEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(EventCreationEvent eventCreationEvent) {
        // 예약 스케줄링
        Event event = eventCreationEvent.getEvent();
        //        try {
        eventRegisterJob.registerJob(event.getId(), event.getDateTimePeriod().getStartAt());
        //        } catch (Exception e) {
        //            log.info("스케줄링 실패 : " + e.getMessage());
        //            throw EventCretionRegisterBatchException.EXCEPTION;
        //        }
    }
}
