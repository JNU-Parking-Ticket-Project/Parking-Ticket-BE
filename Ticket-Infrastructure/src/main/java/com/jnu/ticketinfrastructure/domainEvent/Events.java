package com.jnu.ticketinfrastructure.domainEvent;


import org.springframework.context.ApplicationEventPublisher;

public class Events {
    private static ThreadLocal<ApplicationEventPublisher> publisherLocal = new ThreadLocal<>();

    public static void raise(InfrastructureEvent event) {
        if (event == null) return;

        if (publisherLocal.get() != null) {
            publisherLocal.get().publishEvent(event);
        }
    }

    public static void setPublisher(ApplicationEventPublisher publisher) {
        publisherLocal.set(publisher);
    }

    public static void reset() {
        publisherLocal.remove();
    }
}
