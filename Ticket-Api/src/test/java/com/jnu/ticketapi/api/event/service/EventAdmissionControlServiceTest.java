package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventAdmissionControlServiceTest {

    private static final long EVENT_ID = 10L;

    private EventAdaptor eventAdaptor;
    private EventAdmissionStateCache eventAdmissionStateCache;
    private EventAdmissionControlService service;

    @BeforeEach
    void setUp() {
        eventAdaptor = mock(EventAdaptor.class);
        eventAdmissionStateCache = mock(EventAdmissionStateCache.class);
        service = new EventAdmissionControlService(eventAdaptor, eventAdmissionStateCache);
    }

    @Test
    void cachesModeAfterActivatingDatabaseFallback() {
        Event event = mock(Event.class);
        when(eventAdaptor.findByIdForUpdate(EVENT_ID)).thenReturn(event);
        when(event.getAdmissionMode())
                .thenReturn(EventAdmissionMode.REDIS, EventAdmissionMode.DB_FALLBACK);
        when(event.getAdmissionEpoch()).thenReturn(8L);

        service.activateDatabaseFallback(EVENT_ID, null);

        verify(event).activateDatabaseAdmissionFallback();
        verify(eventAdmissionStateCache)
                .putAfterCommit(
                        EVENT_ID,
                        new EventAdmissionStateCache.AdmissionState(
                                EventAdmissionMode.DB_FALLBACK, 8L));
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
