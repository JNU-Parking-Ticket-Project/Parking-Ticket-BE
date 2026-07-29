package com.jnu.ticketapi.api.event.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.jnu.ticketcommon.utils.Result;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        ReflectionTestUtils.setField(
                openEventUseCase, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("관리자 OPEN 요청도 Redis 재고 초기화 후 상태를 변경한다")
    void executeInitializesRedisBeforeManualOpen() {
        when(eventAdaptor.findReadyEvent()).thenReturn(Result.success(event));
        when(event.getId()).thenReturn(3L);
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));

        openEventUseCase.execute();

        InOrder order = inOrder(sectorAdaptor, waitingQueueService, eventAdaptor);
        order.verify(eventAdaptor).findReadyEvent();
        order.verify(sectorAdaptor).findByEventId(3L);
        order.verify(waitingQueueService).initializeEventStock(3L, List.of(sector));
        order.verify(eventAdaptor).updateEventStatus(event, EventStatus.OPEN);
    }
}
