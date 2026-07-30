package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationAdmissionCoordinatorTest {

    private static final String STREAM_KEY = "쿠폰 발급 스트림:{10}";

    @Mock private RegistrationResultPersistenceService registrationResultPersistenceService;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private Registration registration;
    @Mock private Sector sector;

    private RegistrationAdmissionCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator =
                new RegistrationAdmissionCoordinator(registrationResultPersistenceService);
        ReflectionTestUtils.setField(coordinator, "waitingQueueService", waitingQueueService);
        org.mockito.Mockito.lenient().when(sector.getId()).thenReturn(20L);
        org.mockito.Mockito.lenient()
                .when(waitingQueueService.eventStreamKey(10L))
                .thenReturn(STREAM_KEY);
    }

    @Test
    @DisplayName("Redis 예약 성공은 DB 확정 서비스까지 완료한 결과를 반환한다")
    void admitsWithRedisAndDatabaseCommit() throws Exception {
        StockReservationResult reserved =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 2);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY, registration, 30L, sector, 10L))
                .thenReturn(reserved);
        when(registrationResultPersistenceService.persistRedisReservation(
                        org.mockito.ArgumentMatchers.eq(registration),
                        org.mockito.ArgumentMatchers.eq(30L),
                        org.mockito.ArgumentMatchers.eq(20L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.eq(reserved),
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(reserved);

        StockReservationResult result =
                coordinator.admit(registration, 30L, sector, 10L);

        assertThat(result).isSameAs(reserved);
        assertThat(coordinator.isDatabaseFallback(10L)).isFalse();
    }

    @Test
    @DisplayName("Redis 연결 실패 후 해당 이벤트는 DB fallback을 유지한다")
    void keepsDatabaseFallbackAfterRedisConnectionFailure() throws Exception {
        StockReservationResult fallback =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 2);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY, registration, 30L, sector, 10L))
                .thenThrow(new RedisConnectionFailureException("connection refused"));
        when(registrationResultPersistenceService.persistWithDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(registration),
                        org.mockito.ArgumentMatchers.eq(30L),
                        org.mockito.ArgumentMatchers.eq(20L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(fallback);

        StockReservationResult first =
                coordinator.admit(registration, 30L, sector, 10L);
        StockReservationResult second =
                coordinator.admit(registration, 30L, sector, 10L);

        assertThat(first).isSameAs(fallback);
        assertThat(second).isSameAs(fallback);
        assertThat(coordinator.isDatabaseFallback(10L)).isTrue();
        verify(waitingQueueService, times(1))
                .reserveAndRegisterQueue(STREAM_KEY, registration, 30L, sector, 10L);
        verify(registrationResultPersistenceService, times(2))
                .persistWithDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(registration),
                        org.mockito.ArgumentMatchers.eq(30L),
                        org.mockito.ArgumentMatchers.eq(20L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("Redis 필수 키 유실도 DB fallback으로 전환한다")
    void fallsBackWhenRedisStateIsUnavailable() throws Exception {
        StockReservationResult fallback =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 2);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY, registration, 30L, sector, 10L))
                .thenReturn(StockReservationResult.unavailable(null));
        when(registrationResultPersistenceService.persistWithDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(registration),
                        org.mockito.ArgumentMatchers.eq(30L),
                        org.mockito.ArgumentMatchers.eq(20L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(fallback);

        StockReservationResult result =
                coordinator.admit(registration, 30L, sector, 10L);

        assertThat(result).isSameAs(fallback);
        assertThat(coordinator.isDatabaseFallback(10L)).isTrue();
    }

    @Test
    @DisplayName("Redis가 비활성화된 서버도 DB fallback으로 신청을 처리한다")
    void usesDatabaseFallbackWhenRedisBeanIsDisabled() throws Exception {
        ReflectionTestUtils.setField(coordinator, "waitingQueueService", null);
        StockReservationResult fallback =
                StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 2);
        when(registrationResultPersistenceService.persistWithDatabaseFallback(
                        org.mockito.ArgumentMatchers.eq(registration),
                        org.mockito.ArgumentMatchers.eq(30L),
                        org.mockito.ArgumentMatchers.eq(20L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(fallback);

        StockReservationResult result =
                coordinator.admit(registration, 30L, sector, 10L);

        assertThat(result).isSameAs(fallback);
    }

    @Test
    @DisplayName("Redis 재고가 정상적으로 소진된 경우에는 fallback으로 전환하지 않는다")
    void keepsRedisModeWhenStockIsEmpty() throws Exception {
        StockReservationResult noStock = StockReservationResult.noStock(0);
        when(waitingQueueService.reserveAndRegisterQueue(
                        STREAM_KEY, registration, 30L, sector, 10L))
                .thenReturn(noStock);

        StockReservationResult result =
                coordinator.admit(registration, 30L, sector, 10L);

        assertThat(result).isSameAs(noStock);
        assertThat(coordinator.isDatabaseFallback(10L)).isFalse();
    }
}
