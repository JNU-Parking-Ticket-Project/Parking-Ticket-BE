package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.events.exception.NotOpenEventStatusException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventWithDrawUseCaseTest {

    @Mock private RegistrationAdmissionCoordinator registrationAdmissionCoordinator;
    @Mock private EventAdaptor eventAdaptor;
    @Mock private Event event;
    @Mock private Sector sector;

    private EventWithDrawUseCase eventWithDrawUseCase;

    @BeforeEach
    void setUp() {
        eventWithDrawUseCase =
                new EventWithDrawUseCase(eventAdaptor, registrationAdmissionCoordinator);
    }

    @Test
    @DisplayName("신청 조정기가 DB까지 확정한 예약 결과를 반환한다")
    void issueEventReturnsCommittedReservation() throws Exception {
        Registration registration = registration();
        StockReservationResult result =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 299);
        givenOpenEvent();
        when(registrationAdmissionCoordinator.admit(registration, 100L, sector, 10L))
                .thenReturn(result);

        StockReservationResult actual =
                eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L);

        assertThat(actual).isSameAs(result);
    }

    @Test
    @DisplayName("신청 조정기 결과가 잔여여석 없음이면 기존 예외로 변환한다")
    void issueEventThrowsNoStock() throws Exception {
        Registration registration = registration();
        givenOpenEvent();
        when(registrationAdmissionCoordinator.admit(registration, 100L, sector, 10L))
                .thenReturn(StockReservationResult.noStock(0));

        assertThatThrownBy(() -> eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L))
                .isSameAs(NoEventStockLeftException.EXCEPTION);
    }

    @Test
    @DisplayName("신청 조정기 결과가 중복이면 기존 중복 예외로 변환한다")
    void issueEventThrowsDuplicate() throws Exception {
        Registration registration = registration();
        givenOpenEvent();
        when(registrationAdmissionCoordinator.admit(registration, 100L, sector, 10L))
                .thenReturn(StockReservationResult.duplicate(299));

        assertThatThrownBy(() -> eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L))
                .isSameAs(AlreadyExistRegistrationException.EXCEPTION);
    }

    @Test
    @DisplayName("신청 조정기에서 종료를 확인하면 이벤트 미오픈 예외로 변환한다")
    void issueEventThrowsNotOpenWhenAdmissionIsClosed() throws Exception {
        Registration registration = registration();
        givenOpenEvent();
        when(registrationAdmissionCoordinator.admit(registration, 100L, sector, 10L))
                .thenReturn(StockReservationResult.closed(17));

        assertThatThrownBy(() -> eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L))
                .isSameAs(NotOpenEventStatusException.EXCEPTION);
    }

    @Test
    @DisplayName("기존 RECEIVED 신청은 이벤트 종료 뒤에도 멱등 Redis 결정을 재개한다")
    void resumesExistingAdmissionWithoutRequiringOpenStatus() throws Exception {
        Registration registration = registration();
        StockReservationResult result =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 299);
        when(sector.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(10L);
        when(registrationAdmissionCoordinator.admit(registration, 100L, sector, 10L))
                .thenReturn(result);

        StockReservationResult actual =
                eventWithDrawUseCase.resumeExistingAdmission(
                        registration, 100L, sector, 10L);

        assertThat(actual).isSameAs(result);
        verify(eventAdaptor, never()).findById(10L);
    }

    @Test
    @DisplayName("선택한 구간이 요청 이벤트 소속이 아니면 신청 조정기를 호출하지 않는다")
    void issueEventRejectsSectorFromAnotherEvent() throws Exception {
        Registration registration = registration();
        Event otherEvent = org.mockito.Mockito.mock(Event.class);
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(event.getEventStatus()).thenReturn(EventStatus.OPEN);
        when(sector.getEvent()).thenReturn(otherEvent);
        when(otherEvent.getId()).thenReturn(11L);

        assertThatThrownBy(() -> eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L))
                .isSameAs(NotFoundSectorException.EXCEPTION);

        verify(registrationAdmissionCoordinator, never())
                .admit(registration, 100L, sector, 10L);
    }

    private void givenOpenEvent() {
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(event.getEventStatus()).thenReturn(EventStatus.OPEN);
        when(sector.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(10L);
    }

    private Registration registration() {
        return Registration.builder()
                .email("student@jnu.ac.kr")
                .name("학생")
                .studentNum("20240001")
                .affiliation("공과대학")
                .department("컴퓨터공학과")
                .carNum("12가3456")
                .isLight(false)
                .phoneNum("010-0000-0000")
                .createdAt(LocalDateTime.of(2026, 7, 31, 10, 0))
                .isSaved(false)
                .eventId(10L)
                .build();
    }
}
