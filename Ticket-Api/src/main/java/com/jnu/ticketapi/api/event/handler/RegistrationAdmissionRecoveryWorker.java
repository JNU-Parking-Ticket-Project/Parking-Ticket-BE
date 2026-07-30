package com.jnu.ticketapi.api.event.handler;


import com.jnu.ticketapi.api.event.service.RegistrationAdmissionCoordinator;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("${ableRedis:true}")
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
            registrationAdmissionCoordinator.activateDatabaseFallback(event.getId(), null);
            registrationAdmissionCoordinator.recover(event.getId());
        } catch (NotFoundEventException ignored) {
            log.info("No OPEN event admission state to recover");
        }
    }

    @Scheduled(
            fixedDelayString = "${redis.admission.recovery-fixed-delay-ms:1000}",
            initialDelayString = "${redis.admission.recovery-initial-delay-ms:1000}")
    public void recoverFallbackEvents() {
        registrationAdmissionCoordinator.fallbackEventIds().forEach(this::recoverIfOpen);
    }

    private void recoverIfOpen(Long eventId) {
        try {
            Event event = eventAdaptor.findById(eventId);
            if (event.getEventStatus() == EventStatus.OPEN) {
                registrationAdmissionCoordinator.recover(eventId);
            }
        } catch (NotFoundEventException ignored) {
            log.info(
                    "Fallback event no longer exists; Redis recovery skipped. eventId: {}",
                    eventId);
        }
    }
}
