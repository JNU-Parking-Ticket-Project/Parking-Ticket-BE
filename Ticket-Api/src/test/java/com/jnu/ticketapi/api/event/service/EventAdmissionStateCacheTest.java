package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.event.EventStatusChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventAdmissionStateCacheTest {

    private static final long EVENT_ID = 10L;
    private EventAdaptor eventAdaptor;
    private Event event;
    private EventAdmissionStateCache cache;

    @BeforeEach
    void setUp() {
        eventAdaptor = mock(EventAdaptor.class);
        event = mock(Event.class);
        cache = new EventAdmissionStateCache(eventAdaptor);

        when(eventAdaptor.findById(EVENT_ID)).thenReturn(event);
        when(event.getAdmissionMode()).thenReturn(EventAdmissionMode.REDIS);
        when(event.getAdmissionEpoch()).thenReturn(7L);
    }

    @Test
    void reusesAdmissionStateUntilExplicitInvalidation() {
        EventAdmissionStateCache.AdmissionState first = cache.get(EVENT_ID);
        EventAdmissionStateCache.AdmissionState second = cache.get(EVENT_ID);

        assertThat(second).isSameAs(first);
        verify(eventAdaptor).findById(EVENT_ID);
    }

    @Test
    void reloadsAdmissionStateAfterExplicitInvalidation() {
        cache.get(EVENT_ID);
        cache.invalidate(EVENT_ID);
        cache.get(EVENT_ID);

        verify(eventAdaptor, times(2)).findById(EVENT_ID);
    }

    @Test
    void reusesResolvedModeWhileAuthoritativeEpochIsUnchanged() {
        EventAdmissionStateCache.AdmissionState first =
                cache.resolve(EVENT_ID, 7L, () -> EventAdmissionMode.REDIS);
        EventAdmissionStateCache.AdmissionState second =
                cache.resolve(
                        EVENT_ID,
                        7L,
                        () -> {
                            throw new AssertionError("캐시 적중 시 처리 모드를 다시 읽으면 안 됩니다.");
                        });

        assertThat(second).isSameAs(first);
    }

    @Test
    void refreshesResolvedModeWhenAuthoritativeEpochChanges() {
        cache.resolve(EVENT_ID, 7L, () -> EventAdmissionMode.REDIS);

        EventAdmissionStateCache.AdmissionState refreshed =
                cache.resolve(EVENT_ID, 8L, () -> EventAdmissionMode.DB_FALLBACK);

        assertThat(refreshed.admissionMode()).isEqualTo(EventAdmissionMode.DB_FALLBACK);
        assertThat(refreshed.admissionEpoch()).isEqualTo(8L);
    }

    @Test
    void storesFallbackModeImmediatelyWhenNoTransactionIsActive() {
        EventAdmissionStateCache.AdmissionState fallback =
                new EventAdmissionStateCache.AdmissionState(EventAdmissionMode.DB_FALLBACK, 8L);

        cache.putAfterCommit(EVENT_ID, fallback);

        assertThat(cache.get(EVENT_ID)).isSameAs(fallback);
    }

    @Test
    void invalidatesAdmissionStateWhenEventStatusChanges() {
        cache.get(EVENT_ID);
        EventStatusChangeEvent statusChangeEvent = mock(EventStatusChangeEvent.class);
        when(statusChangeEvent.getEventId()).thenReturn(EVENT_ID);

        cache.handleEventStatusChange(statusChangeEvent);
        cache.get(EVENT_ID);

        verify(eventAdaptor, times(2)).findById(EVENT_ID);
    }
}
