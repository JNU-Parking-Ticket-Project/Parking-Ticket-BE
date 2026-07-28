package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.exception.AlreadyExistRegistrationException;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventWithDrawUseCaseTest {

    private static final String EVENT_STREAM_KEY = "쿠폰 발급 스트림:{10}";

    @Mock private WaitingQueueService waitingQueueService;
    @Mock private EventAdaptor eventAdaptor;
    @Mock private Event event;
    @Mock private Sector sector;

    private EventWithDrawUseCase eventWithDrawUseCase;

    @BeforeEach
    void setUp() {
        eventWithDrawUseCase = new EventWithDrawUseCase(eventAdaptor);
        ReflectionTestUtils.setField(
                eventWithDrawUseCase, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("Redis 예약 성공 시 예약 결과를 반환하고 Stream 저장을 위임한다")
    void issueEventReturnsReservedResult() throws Exception {
        Registration registration = registration();
        StockReservationResult reservationResult =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 299);
        givenOpenEvent();
        when(waitingQueueService.reserveAndRegisterQueue(
                        eq(EVENT_STREAM_KEY), eq(registration), eq(100L), eq(sector), eq(10L)))
                .thenReturn(reservationResult);

        StockReservationResult result =
                eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L);

        assertThat(result).isSameAs(reservationResult);
        verify(waitingQueueService)
                .reserveAndRegisterQueue(EVENT_STREAM_KEY, registration, 100L, sector, 10L);
    }

    @Test
    @DisplayName("Redis 예약 결과가 잔여여석 없음이면 기존 잔여여석 예외로 변환한다")
    void issueEventThrowsNoStockWhenRedisStockIsEmpty() throws Exception {
        Registration registration = registration();
        givenOpenEvent();
        when(waitingQueueService.reserveAndRegisterQueue(
                        EVENT_STREAM_KEY, registration, 100L, sector, 10L))
                .thenReturn(StockReservationResult.noStock(0));

        assertThatThrownBy(() -> eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L))
                .isSameAs(NoEventStockLeftException.EXCEPTION);
    }

    @Test
    @DisplayName("Redis 예약 결과가 중복이면 기존 중복 신청 예외로 변환한다")
    void issueEventThrowsDuplicateWhenRedisReservationIsDuplicate() throws Exception {
        Registration registration = registration();
        givenOpenEvent();
        when(waitingQueueService.reserveAndRegisterQueue(
                        EVENT_STREAM_KEY, registration, 100L, sector, 10L))
                .thenReturn(StockReservationResult.duplicate(299));

        assertThatThrownBy(() -> eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L))
                .isSameAs(AlreadyExistRegistrationException.EXCEPTION);
    }

    @Test
    @DisplayName("선택한 구간이 요청 이벤트 소속이 아니면 Redis 예약을 거부한다")
    void issueEventRejectsSectorFromAnotherEvent() throws Exception {
        Registration registration = registration();
        Event otherEvent = org.mockito.Mockito.mock(Event.class);
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(event.getEventStatus()).thenReturn(EventStatus.OPEN);
        when(sector.getEvent()).thenReturn(otherEvent);
        when(otherEvent.getId()).thenReturn(11L);

        assertThatThrownBy(() -> eventWithDrawUseCase.issueEvent(registration, 100L, sector, 10L))
                .isSameAs(NotFoundSectorException.EXCEPTION);

        verify(waitingQueueService, never())
                .reserveAndRegisterQueue(
                        org.mockito.ArgumentMatchers.anyString(),
                        eq(registration),
                        eq(100L),
                        eq(sector),
                        eq(10L));
    }

    private void givenOpenEvent() {
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(event.getId()).thenReturn(10L);
        when(event.getEventStatus()).thenReturn(EventStatus.OPEN);
        when(sector.getEvent()).thenReturn(event);
        when(waitingQueueService.eventStreamKey(10L)).thenReturn(EVENT_STREAM_KEY);
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
                .createdAt(LocalDateTime.of(2026, 6, 25, 10, 0))
                .isSaved(false)
                .eventId(10L)
                .build();
    }
}
