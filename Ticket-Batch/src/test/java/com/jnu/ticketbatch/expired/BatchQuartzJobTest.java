package com.jnu.ticketbatch.expired;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
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
class BatchQuartzJobTest {

    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private Sector sector;

    private BatchQuartzJob batchQuartzJob;

    @BeforeEach
    void setUp() {
        batchQuartzJob = new BatchQuartzJob();
        ReflectionTestUtils.setField(batchQuartzJob, "sectorAdaptor", sectorAdaptor);
        ReflectionTestUtils.setField(batchQuartzJob, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("이벤트 종료 시 Redis 잔여재고를 DB에 반영하고 drain 기간 뒤 만료시킨다")
    void syncAndExpireRedisStockUpdatesDatabaseBeforeExpiration() {
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(sector.getId()).thenReturn(2L);
        when(waitingQueueService.findRemainingStock(3L, 2L)).thenReturn(Optional.of(0));

        batchQuartzJob.syncAndExpireRedisStock(3L);

        InOrder redisOrder = inOrder(waitingQueueService);
        redisOrder
                .verify(waitingQueueService)
                .markEventStockClosed(3L, Duration.ofMinutes(5));
        redisOrder.verify(waitingQueueService).findRemainingStock(3L, 2L);
        redisOrder
                .verify(waitingQueueService)
                .expireEventStockKeys(3L, Duration.ofMinutes(5));
        verify(sector).syncRemainingAmount(0);
        verify(sectorAdaptor).save(sector);
        verify(waitingQueueService, never()).deleteEventStockKeys(3L);
    }

    @Test
    @DisplayName("Redis가 비활성화된 환경에서는 종료 재고 동기화를 건너뛴다")
    void syncAndExpireRedisStockSkipsWhenRedisIsDisabled() {
        ReflectionTestUtils.setField(batchQuartzJob, "waitingQueueService", null);

        batchQuartzJob.syncAndExpireRedisStock(3L);

        verify(sectorAdaptor, never()).findByEventId(3L);
    }
}
