package com.jnu.ticketapi.api.event.handler;


import com.jnu.ticketapi.api.event.service.RegistrationAdmissionCoordinator;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "registration.admission.recovery.enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RegistrationAdmissionRecoveryWorker {

    private final EventAdaptor eventAdaptor;
    private final RegistrationAdmissionCoordinator registrationAdmissionCoordinator;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void restoreOpenEventAdmission() {
        try {
            Event event = eventAdaptor.findOpenEvent();
            registrationAdmissionCoordinator.restoreOpenEventAdmission(event.getId());
        } catch (NotFoundEventException ignored) {
            log.info("No OPEN event admission state to recover");
        }
    }

    @Scheduled(
            fixedDelayString = "${redis.admission.recovery-fixed-delay-ms:1000}",
            initialDelayString = "${redis.admission.recovery-initial-delay-ms:1000}")
    public void recoverUnavailableEvents() {
        registrationAdmissionCoordinator.reconcileConfirmedRegistrations();
        registrationAdmissionCoordinator.recoveryEventIds().forEach(this::recoverFallbackEvent);
    }

    private void recoverFallbackEvent(Long eventId) {
        try {
            eventAdaptor.findById(eventId);
            registrationAdmissionCoordinator.recover(eventId);
        } catch (NotFoundEventException ignored) {
            log.info(
                    "Fallback event no longer exists; Redis recovery skipped. eventId: {}",
                    eventId);
        }
    }
}
