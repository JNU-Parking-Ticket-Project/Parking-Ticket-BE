package com.jnu.ticketbatch;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.event.EventStatusChangeEvent;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketinfrastructure.stream.RedisStreamConsumerManager;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${ableRedis:true}")
@ConditionalOnProperty(
        value = "redis.stream.consumer.lifecycle-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EventStreamConsumerLifecycleHandler {

    private final EventAdaptor eventAdaptor;
    private final RedisStreamConsumerManager streamConsumerManager;

    @Value("${redis.stream.consumer.startup-drain-timeout-ms:300000}")
    private long startupDrainTimeoutMillis = 300_000L;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public void restoreOpenEventSubscription() {
        try {
            Event event = eventAdaptor.findOpenEvent();
            streamConsumerManager.start(event.getId());
        } catch (NotFoundEventException ignored) {
            log.info("No OPEN event Stream subscription to restore");
        }

        eventAdaptor.findClosedEvents().forEach(this::restoreDrain);
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
            if (!streamConsumerManager.requestDrain(event.getId())) {
                log.error("Redis Stream drain request failed. eventId: {}", event.getId());
            }
        }
    }

    private void restoreDrain(Event event) {
        Long eventId = event.getId();
        Duration timeout = Duration.ofMillis(Math.max(1L, startupDrainTimeoutMillis));
        if (!streamConsumerManager.requestDrain(eventId)
                || !streamConsumerManager.awaitDrainCompletion(eventId, timeout)) {
            throw new IllegalStateException(
                    "CLOSED 이벤트의 Redis Stream drain을 복원하지 못했습니다. eventId=" + eventId);
        }
        log.info("CLOSED event Stream drain restored. eventId: {}", eventId);
    }
}
