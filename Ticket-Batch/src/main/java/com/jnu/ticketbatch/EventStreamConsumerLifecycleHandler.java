package com.jnu.ticketbatch;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.event.EventStatusChangeEvent;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketinfrastructure.stream.RedisStreamConsumerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${ableRedis:true}")
public class EventStreamConsumerLifecycleHandler {

    private final EventAdaptor eventAdaptor;
    private final RedisStreamConsumerManager streamConsumerManager;

    @EventListener(ApplicationReadyEvent.class)
    public void restoreOpenEventSubscription() {
        try {
            Event event = eventAdaptor.findOpenEvent();
            streamConsumerManager.start(event.getId());
        } catch (NotFoundEventException ignored) {
            log.info("No OPEN event Stream subscription to restore");
        }
    }

    @TransactionalEventListener(
            classes = EventStatusChangeEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EventStatusChangeEvent eventStatusChangeEvent) {
        Event event = eventAdaptor.findById(eventStatusChangeEvent.getEventId());
        if (event.getEventStatus() == EventStatus.OPEN) {
            streamConsumerManager.start(event.getId());
            return;
        }
        if (event.getEventStatus() == EventStatus.CLOSED) {
            streamConsumerManager.requestDrain(event.getId());
        }
    }
}
