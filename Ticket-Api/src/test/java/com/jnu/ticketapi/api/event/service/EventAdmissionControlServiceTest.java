package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.event.event.DatabaseFallbackActivatedEvent;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class EventAdmissionControlServiceTest {

    private static final long EVENT_ID = 10L;

    private EventAdaptor eventAdaptor;
    private EventAdmissionStateCache eventAdmissionStateCache;
    private ApplicationEventPublisher eventPublisher;
    private EventAdmissionControlService service;

    @BeforeEach
    void setUp() {
        eventAdaptor = mock(EventAdaptor.class);
        eventAdmissionStateCache = mock(EventAdmissionStateCache.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service =
                new EventAdmissionControlService(
                        eventAdaptor, eventAdmissionStateCache, eventPublisher);
    }

    @Test
    void cachesModeAndPublishesAlertAfterActivatingDatabaseFallback() {
        Event event = mock(Event.class);
        IllegalStateException cause = new IllegalStateException("Redis connection refused");
        when(eventAdaptor.findByIdForUpdate(EVENT_ID)).thenReturn(event);
        when(event.getAdmissionMode())
                .thenReturn(EventAdmissionMode.REDIS, EventAdmissionMode.DB_FALLBACK);
        when(event.getAdmissionEpoch()).thenReturn(8L);

        service.activateDatabaseFallback(EVENT_ID, cause);

        verify(event).activateDatabaseAdmissionFallback();
        verify(eventAdmissionStateCache)
                .putAfterCommit(
                        EVENT_ID,
                        new EventAdmissionStateCache.AdmissionState(
                                EventAdmissionMode.DB_FALLBACK, 8L));
        ArgumentCaptor<DatabaseFallbackActivatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DatabaseFallbackActivatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventId()).isEqualTo(EVENT_ID);
        assertThat(eventCaptor.getValue().admissionEpoch()).isEqualTo(8L);
        assertThat(eventCaptor.getValue().cause())
                .isEqualTo("IllegalStateException: Redis connection refused");
    }

    @Test
    void doesNotPublishDuplicateAlertWhenFallbackWasAlreadyActive() {
        Event event = mock(Event.class);
        when(eventAdaptor.findByIdForUpdate(EVENT_ID)).thenReturn(event);
        when(event.getAdmissionMode()).thenReturn(EventAdmissionMode.DB_FALLBACK);
        when(event.getAdmissionEpoch()).thenReturn(8L);

        service.activateDatabaseFallback(EVENT_ID, new IllegalStateException("retry"));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void readsDatabaseFallbackModeFromLocalStateCache() {
        when(eventAdmissionStateCache.get(EVENT_ID))
                .thenReturn(
                        new EventAdmissionStateCache.AdmissionState(
                                EventAdmissionMode.DB_FALLBACK, 8L));

        assertThat(service.isDatabaseFallback(EVENT_ID)).isTrue();

        verify(eventAdmissionStateCache).get(EVENT_ID);
    }
}
