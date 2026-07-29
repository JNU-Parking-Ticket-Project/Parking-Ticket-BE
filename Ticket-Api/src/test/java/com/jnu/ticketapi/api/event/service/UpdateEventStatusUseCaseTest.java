package com.jnu.ticketapi.api.event.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.event.model.request.UpdateEventStatusRequest;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UpdateEventStatusUseCaseTest {

    @Mock private EventAdaptor eventAdaptor;
    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private Event event;
    @Mock private Sector sector;

    private UpdateEventStatusUseCase updateEventStatusUseCase;

    @BeforeEach
    void setUp() {
        updateEventStatusUseCase = new UpdateEventStatusUseCase(eventAdaptor, sectorAdaptor);
        ReflectionTestUtils.setField(
                updateEventStatusUseCase, "waitingQueueService", waitingQueueService);
        when(eventAdaptor.findById(3L)).thenReturn(event);
    }

    @Test
    @DisplayName("상태 변경 API가 OPEN을 요청하면 Redis 초기화 후 상태를 변경한다")
    void executeInitializesRedisBeforeOpenStatusUpdate() {
        UpdateEventStatusRequest request = request(EventStatus.OPEN);
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(eventAdaptor.updateEventStatus(event, EventStatus.OPEN)).thenReturn(event);

        updateEventStatusUseCase.execute(3L, request);

        InOrder order = inOrder(sectorAdaptor, waitingQueueService, eventAdaptor);
        order.verify(eventAdaptor).findById(3L);
        order.verify(sectorAdaptor).findByEventId(3L);
        order.verify(waitingQueueService).initializeEventStock(3L, List.of(sector));
        order.verify(eventAdaptor).updateEventStatus(event, EventStatus.OPEN);
    }

    @Test
    @DisplayName("CLOSED 상태 변경은 Redis 접수를 먼저 닫고 잔여 재고를 DB에 동기화한다")
    void executeClosesAndSynchronizesRedisStockBeforeStatusUpdate() {
        UpdateEventStatusRequest request = request(EventStatus.CLOSED);
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(sector.getId()).thenReturn(7L);
        when(waitingQueueService.findRemainingStock(3L, 7L)).thenReturn(Optional.of(4));
        when(eventAdaptor.updateEventStatus(event, EventStatus.CLOSED)).thenReturn(event);

        updateEventStatusUseCase.execute(3L, request);

        InOrder order = inOrder(waitingQueueService, sectorAdaptor, sector, eventAdaptor);
        order.verify(eventAdaptor).findById(3L);
        order.verify(waitingQueueService).markEventStockClosed(3L, Duration.ofMinutes(5));
        order.verify(sectorAdaptor).findByEventId(3L);
        order.verify(waitingQueueService).findRemainingStock(3L, 7L);
        order.verify(sector).syncRemainingAmount(4);
        order.verify(sectorAdaptor).save(sector);
        order.verify(waitingQueueService).expireEventStockKeys(3L, Duration.ofMinutes(5));
        order.verify(eventAdaptor).updateEventStatus(event, EventStatus.CLOSED);
    }

    @Test
    @DisplayName("OPEN과 CLOSED 이외 상태 변경은 Redis 재고를 변경하지 않는다")
    void executeDoesNotInitializeRedisForOtherStatuses() {
        UpdateEventStatusRequest request = request(EventStatus.CALCULATING);
        when(eventAdaptor.updateEventStatus(event, EventStatus.CALCULATING)).thenReturn(event);

        updateEventStatusUseCase.execute(3L, request);

        verify(sectorAdaptor, never()).findByEventId(3L);
        verify(waitingQueueService, never())
                .initializeEventStock(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(waitingQueueService, never())
                .markEventStockClosed(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private UpdateEventStatusRequest request(EventStatus status) {
        UpdateEventStatusRequest request = new UpdateEventStatusRequest();
        ReflectionTestUtils.setField(request, "status", status);
        return request;
    }
}
