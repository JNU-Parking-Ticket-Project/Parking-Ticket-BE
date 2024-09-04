package com.jnu.ticketdomain.domains.events;


import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.events.event.EventExpiredEvent;
import java.time.LocalDateTime;
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
public class EventExpiredEventRaiseGateway {

    @SneakyThrows
    @Async
    @TransactionalEventListener(
            classes = EventExpiredEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(Long eventId) {
        log.info(eventId + "EventExpiredEvent occured now " + LocalDateTime.now().toString());
        Events.raise(new EventExpiredEvent(eventId));
    }
}
