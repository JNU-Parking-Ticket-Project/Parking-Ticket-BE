package com.jnu.ticketinfrastructure.domainEvent;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
@Slf4j
public class Events {
    private static ThreadLocal<ApplicationEventPublisher> publisherLocal = new ThreadLocal<>();

    public static void raise(InfrastructureEvent event) {
        log.info("Raising event: {}", event);
        if (publisherLocal.get() != null) {
            publisherLocal.get().publishEvent(event);
            log.info("Event published: " + event);
        } else {
            log.error("No publisher found for event: {}", event);
        }
    }

    public static void setPublisher(ApplicationEventPublisher publisher) {
        publisherLocal.set(publisher);
    }

    public static void reset() {
        publisherLocal.remove();
    }
}
