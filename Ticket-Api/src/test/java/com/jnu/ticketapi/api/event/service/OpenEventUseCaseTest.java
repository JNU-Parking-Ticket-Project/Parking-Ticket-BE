package com.jnu.ticketapi.api.event.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketdomain.domains.events.exception.RedisStockUnavailableException;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OpenEventUseCaseTest {

    @Mock private EventAdaptor eventAdaptor;
    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private Event event;
    @Mock private Sector sector;

    private OpenEventUseCase openEventUseCase;

    @BeforeEach
    void setUp() {
        openEventUseCase = new OpenEventUseCase(eventAdaptor, sectorAdaptor);
        ReflectionTestUtils.setField(openEventUseCase, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("관리자 OPEN 요청도 Redis 재고 초기화 후 상태를 변경한다")
    void executeInitializesRedisBeforeManualOpen() {
        when(eventAdaptor.findReadyEvent()).thenReturn(Result.success(event));
        when(event.getId()).thenReturn(3L);
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(waitingQueueService.initializeEventStock(3L, List.of(sector))).thenReturn(true);

        openEventUseCase.execute();

        InOrder order = inOrder(sectorAdaptor, waitingQueueService, eventAdaptor);
        order.verify(eventAdaptor).findReadyEvent();
        order.verify(sectorAdaptor).findByEventId(3L);
        order.verify(waitingQueueService).initializeEventStock(3L, List.of(sector));
        order.verify(eventAdaptor).updateEventStatus(event, EventStatus.OPEN);
    }

    @Test
    @DisplayName("Redis 초기화가 실패하면 이벤트를 OPEN하지 않는다")
    void executeDoesNotOpenWhenRedisInitializationFails() {
        when(eventAdaptor.findReadyEvent()).thenReturn(Result.success(event));
        when(event.getId()).thenReturn(3L);
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        RedisConnectionFailureException failure =
                new RedisConnectionFailureException("connection refused");
        when(waitingQueueService.initializeEventStock(3L, List.of(sector))).thenThrow(failure);

        org.assertj.core.api.Assertions.assertThatThrownBy(openEventUseCase::execute)
                .isSameAs(RedisStockUnavailableException.EXCEPTION);

        verify(eventAdaptor, org.mockito.Mockito.never())
                .updateEventStatus(event, EventStatus.OPEN);
    }

    @Test
    @DisplayName("Redis 초기화가 false를 반환하면 이벤트를 OPEN하지 않는다")
    void executeDoesNotOpenWhenRedisInitializationReturnsFalse() {
        when(eventAdaptor.findReadyEvent()).thenReturn(Result.success(event));
        when(event.getId()).thenReturn(3L);
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(waitingQueueService.initializeEventStock(3L, List.of(sector))).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(openEventUseCase::execute)
                .isSameAs(RedisStockUnavailableException.EXCEPTION);

        verify(eventAdaptor, org.mockito.Mockito.never())
                .updateEventStatus(event, EventStatus.OPEN);
    }

    @Test
    @DisplayName("READY 이벤트가 없으면 이미 OPEN된 이벤트의 수동 OPEN 재요청도 거부한다")
    void executeRejectsRepeatedOpenRequest() {
        when(eventAdaptor.findReadyEvent())
                .thenReturn(Result.failure(NotFoundEventException.EXCEPTION));

        org.assertj.core.api.Assertions.assertThatThrownBy(openEventUseCase::execute)
                .isSameAs(NotFoundEventException.EXCEPTION);

        verify(sectorAdaptor, org.mockito.Mockito.never()).findByEventId(3L);
        verify(waitingQueueService, org.mockito.Mockito.never())
                .initializeEventStock(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyList());
        verify(eventAdaptor, org.mockito.Mockito.never())
                .updateEventStatus(event, EventStatus.OPEN);
    }

    @Test
    @DisplayName("scale-down 서버는 Redis 없이 이벤트를 OPEN하지 않는다")
    void executeDoesNotOpenWhenRedisIsDisabled() {
        ReflectionTestUtils.setField(openEventUseCase, "waitingQueueService", null);
        when(eventAdaptor.findReadyEvent()).thenReturn(Result.success(event));

        org.assertj.core.api.Assertions.assertThatThrownBy(openEventUseCase::execute)
                .isSameAs(RedisStockUnavailableException.EXCEPTION);

        verify(eventAdaptor, org.mockito.Mockito.never())
                .updateEventStatus(event, EventStatus.OPEN);
    }
}
