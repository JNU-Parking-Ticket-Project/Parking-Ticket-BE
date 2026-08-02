package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventAdmissionControlService {
    private final EventAdaptor eventAdaptor;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activateDatabaseFallback(Long eventId, Throwable cause) {
        Event event = eventAdaptor.findByIdForUpdate(eventId);
        EventAdmissionMode previousMode = event.getAdmissionMode();
        event.activateDatabaseAdmissionFallback();
        if (previousMode != EventAdmissionMode.DB_FALLBACK) {
            if (cause == null) {
                log.warn("Registration admission switched to DB fallback. eventId: {}", eventId);
            } else {
                log.warn(
                        "Registration admission switched to DB fallback. eventId: {}, cause: {}",
                        eventId,
                        cause.toString());
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean isDatabaseFallback(Long eventId) {
        return eventAdaptor.findById(eventId).getAdmissionMode() == EventAdmissionMode.DB_FALLBACK;
    }

    @Transactional(readOnly = true)
    public boolean isOpenForAdmission(Long eventId) {
        return eventAdaptor.findByIdForAdmissionRead(eventId).getEventStatus() == EventStatus.OPEN;
    }

    @Transactional(readOnly = true)
    public Set<Long> fallbackEventIds() {
        return eventAdaptor.findEventsByAdmissionMode(EventAdmissionMode.DB_FALLBACK).stream()
                .map(Event::getId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
