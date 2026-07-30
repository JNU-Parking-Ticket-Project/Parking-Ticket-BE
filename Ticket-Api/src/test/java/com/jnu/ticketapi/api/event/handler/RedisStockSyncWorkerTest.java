package com.jnu.ticketapi.api.event.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedisStockSyncWorkerTest {

    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private WaitingQueueService waitingQueueService;

    private RedisStockSyncWorker redisStockSyncWorker;

    @BeforeEach
    void setUp() {
        redisStockSyncWorker = new RedisStockSyncWorker(sectorAdaptor);
        ReflectionTestUtils.setField(
                redisStockSyncWorker, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("Redis 잔여 재고가 있으면 Sector 재고 필드를 동기화하고 저장한다")
    void syncSectorUpdatesSectorWhenRedisStockExists() {
        Sector sector = sector();
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.of(1));

        redisStockSyncWorker.syncSector(sector);

        assertThat(sector.getSectorCapacity()).isZero();
        assertThat(sector.getReserve()).isEqualTo(1);
        assertThat(sector.getRemainingAmount()).isEqualTo(1);
        verify(sectorAdaptor).save(sector);
    }

    @Test
    @DisplayName("Redis 잔여 재고가 없으면 Sector를 저장하지 않는다")
    void syncSectorDoesNothingWhenRedisStockDoesNotExist() {
        Sector sector = sector();
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.empty());

        redisStockSyncWorker.syncSector(sector);

        verify(sectorAdaptor, never()).save(sector);
    }

    private Sector sector() {
        Event event = Event.builder().title("주차권").sector(java.util.List.of()).build();
        ReflectionTestUtils.setField(event, "id", 10L);

        Sector sector =
                Sector.builder()
                        .sectorNumber("1구간")
                        .name("공과대학")
                        .sectorCapacity(2)
                        .reserve(1)
                        .build();
        ReflectionTestUtils.setField(sector, "id", 20L);
        sector.setEvent(event);
        return sector;
    }
}
