package com.jnu.ticketbatch.expired;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;

import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
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
class BatchQuartzJobTest {

    @Mock private WaitingQueueService waitingQueueService;

    private BatchQuartzJob batchQuartzJob;

    @BeforeEach
    void setUp() {
        batchQuartzJob = new BatchQuartzJob();
        ReflectionTestUtils.setField(batchQuartzJob, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("이벤트 종료는 DB 체크포인트를 유지하고 Redis 접수만 닫아 만료시킨다")
    void closesAndExpiresRedisWithoutUpdatingDatabaseStock() {
        batchQuartzJob.syncAndExpireRedisStock(3L);

        InOrder redisOrder = inOrder(waitingQueueService);
        redisOrder.verify(waitingQueueService).markEventStockClosed(3L, Duration.ofMinutes(5));
        redisOrder.verify(waitingQueueService).expireEventStockKeys(3L, Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("실제 종료 시 Redis 장애가 발생해도 DB 이벤트 종료를 방해하지 않는다")
    void ignoresRedisFailureAtActualEventEnd() {
        org.mockito.Mockito.doThrow(new RedisConnectionFailureException("connection refused"))
                .when(waitingQueueService)
                .markEventStockClosed(3L, Duration.ofMinutes(5));

        assertThatCode(() -> batchQuartzJob.syncAndExpireRedisStock(3L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis가 비활성화된 환경에서는 종료 정리를 건너뛴다")
    void skipsWhenRedisIsDisabled() {
        ReflectionTestUtils.setField(batchQuartzJob, "waitingQueueService", null);

        assertThatCode(() -> batchQuartzJob.syncAndExpireRedisStock(3L)).doesNotThrowAnyException();
    }
}
