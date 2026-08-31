package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.event.EventStatusChangeEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class EventAdmissionStateCache {

    private final EventAdaptor eventAdaptor;
    private final ConcurrentMap<Long, AdmissionState> states = new ConcurrentHashMap<>();

    public AdmissionState get(Long eventId) {
        return states.computeIfAbsent(eventId, this::load);
    }

    public AdmissionState resolve(
            Long eventId, long authoritativeEpoch, Supplier<EventAdmissionMode> authoritativeMode) {
        return states.compute(
                eventId,
                (id, current) -> {
                    if (current != null && current.admissionEpoch() == authoritativeEpoch) {
                        return current;
                    }
                    return new AdmissionState(authoritativeMode.get(), authoritativeEpoch);
                });
    }

    public void putAfterCommit(Long eventId, AdmissionState state) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            put(eventId, state);
                        }
                    });
            return;
        }
        put(eventId, state);
    }

    @TransactionalEventListener(
            classes = EventStatusChangeEvent.class,
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void handleEventStatusChange(EventStatusChangeEvent event) {
        invalidate(event.getEventId());
    }

    void invalidate(Long eventId) {
        states.remove(eventId);
    }

    private AdmissionState load(Long eventId) {
        Event event = eventAdaptor.findById(eventId);
        long admissionEpoch = event.getAdmissionEpoch() == null ? 0L : event.getAdmissionEpoch();
        return new AdmissionState(event.getAdmissionMode(), admissionEpoch);
    }

    private void put(Long eventId, AdmissionState state) {
        states.put(eventId, state);
    }

    public record AdmissionState(EventAdmissionMode admissionMode, long admissionEpoch) {}
}
