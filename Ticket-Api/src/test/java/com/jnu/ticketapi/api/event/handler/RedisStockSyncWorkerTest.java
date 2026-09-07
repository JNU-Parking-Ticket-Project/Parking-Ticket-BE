package com.jnu.ticketapi.api.event.handler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.event.service.RegistrationAdmissionCoordinator;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
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
    @Mock private RegistrationAdmissionCoordinator registrationAdmissionCoordinator;

    private RedisStockSyncWorker redisStockSyncWorker;

    @BeforeEach
    void setUp() {
        redisStockSyncWorker =
                new RedisStockSyncWorker(sectorAdaptor, registrationAdmissionCoordinator);
        ReflectionTestUtils.setField(
                redisStockSyncWorker, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("Redis와 DB 체크포인트가 같으면 Redis 모드를 유지한다")
    void keepsRedisModeWhenCheckpointsMatch() {
        Sector sector = sector();
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.of(3));
        when(waitingQueueService.findAssignedPosition(10L, 20L)).thenReturn(Optional.of(0));
        when(registrationAdmissionCoordinator.findMaxDecidedPosition(20L)).thenReturn(0);

        redisStockSyncWorker.syncSector(sector);

        verify(registrationAdmissionCoordinator, never())
                .activateDatabaseFallback(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(sectorAdaptor, never()).save(sector);
    }

    @Test
    @DisplayName("Redis 예약 직후 DB commit 전의 일시적 재고 차이는 fallback으로 오판하지 않는다")
    void keepsRedisModeWhenRedisIsTemporarilyAhead() {
        Sector sector = sector();
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.of(2));
        when(waitingQueueService.findAssignedPosition(10L, 20L)).thenReturn(Optional.of(1));
        when(registrationAdmissionCoordinator.findMaxDecidedPosition(20L)).thenReturn(0);

        redisStockSyncWorker.syncSector(sector);

        verify(registrationAdmissionCoordinator, never())
                .activateDatabaseFallback(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(sectorAdaptor, never()).save(sector);
    }

    @Test
    @DisplayName("Redis가 DB보다 많은 재고를 가지면 초과 접수를 막고 복구를 시작한다")
    void activatesRecoveryWhenRedisCheckpointCanOversell() {
        Sector sector = sector();
        sector.syncRemainingAmount(2);
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.of(3));
        when(waitingQueueService.findAssignedPosition(10L, 20L)).thenReturn(Optional.of(0));
        when(registrationAdmissionCoordinator.findMaxDecidedPosition(20L)).thenReturn(0);

        redisStockSyncWorker.syncSector(sector);

        verify(registrationAdmissionCoordinator)
                .activateDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.isA(IllegalStateException.class));
        verify(sectorAdaptor, never()).save(sector);
    }

    @Test
    @DisplayName("Redis stock key가 없으면 신청을 차단하고 복구를 시작한다")
    void activatesRecoveryWhenRedisStockIsMissing() {
        Sector sector = sector();
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.empty());
        when(waitingQueueService.findAssignedPosition(10L, 20L)).thenReturn(Optional.of(0));
        when(registrationAdmissionCoordinator.findMaxDecidedPosition(20L)).thenReturn(0);

        redisStockSyncWorker.syncSector(sector);

        verify(registrationAdmissionCoordinator)
                .activateDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.isA(IllegalStateException.class));
    }

    @Test
    @DisplayName("Redis position key가 없으면 DB fallback으로 전환한다")
    void activatesRecoveryWhenRedisPositionIsMissing() {
        Sector sector = sector();
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.of(3));
        when(waitingQueueService.findAssignedPosition(10L, 20L)).thenReturn(Optional.empty());
        when(registrationAdmissionCoordinator.findMaxDecidedPosition(20L)).thenReturn(0);

        redisStockSyncWorker.syncSector(sector);

        verify(registrationAdmissionCoordinator)
                .activateDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.isA(IllegalStateException.class));
    }

    @Test
    @DisplayName("Redis remaining과 position의 합이 발급량과 다르면 DB fallback으로 전환한다")
    void activatesRecoveryWhenRedisStockAndPositionDoNotMatchIssueAmount() {
        Sector sector = sector();
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.of(1));
        when(waitingQueueService.findAssignedPosition(10L, 20L)).thenReturn(Optional.of(1));
        when(registrationAdmissionCoordinator.findMaxDecidedPosition(20L)).thenReturn(1);

        redisStockSyncWorker.syncSector(sector);

        verify(registrationAdmissionCoordinator)
                .activateDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.isA(IllegalStateException.class));
    }

    @Test
    @DisplayName("Redis position이 DB 확정 high-water보다 뒤처지면 DB fallback으로 전환한다")
    void activatesRecoveryWhenRedisPositionFallsBehindDatabaseHighWater() {
        Sector sector = sector();
        when(waitingQueueService.findRemainingStock(10L, 20L)).thenReturn(Optional.of(2));
        when(waitingQueueService.findAssignedPosition(10L, 20L)).thenReturn(Optional.of(1));
        when(registrationAdmissionCoordinator.findMaxDecidedPosition(20L)).thenReturn(2);

        redisStockSyncWorker.syncSector(sector);

        verify(registrationAdmissionCoordinator)
                .activateDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.isA(IllegalStateException.class));
    }

    private Sector sector() {
        Event event = Event.builder().title("주차권").sector(java.util.List.of()).build();
        ReflectionTestUtils.setField(event, "id", 10L);
        ReflectionTestUtils.setField(event, "eventStatus", EventStatus.OPEN);

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
