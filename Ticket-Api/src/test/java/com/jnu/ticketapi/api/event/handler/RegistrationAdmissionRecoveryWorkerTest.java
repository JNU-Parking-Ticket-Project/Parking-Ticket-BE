package com.jnu.ticketapi.api.event.handler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.event.service.RegistrationAdmissionCoordinator;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationAdmissionRecoveryWorkerTest {

    @Mock private EventAdaptor eventAdaptor;
    @Mock private RegistrationAdmissionCoordinator registrationAdmissionCoordinator;
    @Mock private Event event;
    @Mock private Event closedEvent;

    private RegistrationAdmissionRecoveryWorker worker;

    @BeforeEach
    void setUp() {
        worker =
                new RegistrationAdmissionRecoveryWorker(
                        eventAdaptor, registrationAdmissionCoordinator);
    }

    @Test
    @DisplayName("서버 기동 시 OPEN 이벤트의 Redis 상태 검증과 저널 복구를 coordinator에 위임한다")
    void restoresOpenEventAdmissionOnStartup() {
        when(eventAdaptor.findOpenEvent()).thenReturn(event);
        when(event.getId()).thenReturn(10L);

        worker.restoreOpenEventAdmission();

        verify(registrationAdmissionCoordinator).restoreOpenEventAdmission(10L);
        verify(registrationAdmissionCoordinator, never()).activateDatabaseFallback(10L, null);
    }

    @Test
    @DisplayName("OPEN 이벤트가 없으면 서버 기동 복구를 건너뛴다")
    void skipsStartupRecoveryWithoutOpenEvent() {
        when(eventAdaptor.findOpenEvent()).thenThrow(NotFoundEventException.EXCEPTION);

        worker.restoreOpenEventAdmission();
    }

    @Test
    @DisplayName("주기 작업은 확정 저널과 종료된 이벤트를 포함한 DB fallback 저널을 복구한다")
    void recoversUnavailableEventsPeriodically() {
        when(registrationAdmissionCoordinator.recoveryEventIds()).thenReturn(Set.of(10L, 11L));
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(eventAdaptor.findById(11L)).thenReturn(closedEvent);

        worker.recoverUnavailableEvents();

        verify(registrationAdmissionCoordinator).reconcileConfirmedRegistrations();
        verify(registrationAdmissionCoordinator).recover(10L);
        verify(registrationAdmissionCoordinator).recover(11L);
    }
}
