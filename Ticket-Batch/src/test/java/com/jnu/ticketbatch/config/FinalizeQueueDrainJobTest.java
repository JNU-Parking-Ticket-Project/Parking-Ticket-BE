package com.jnu.ticketbatch.config;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FinalizeQueueDrainJobTest {

    @Mock private WaitingQueueService waitingQueueService;
    @Mock private JobExecutionContext context;

    private FinalizeQueueDrainJob finalizeQueueDrainJob;

    @BeforeEach
    void setUp() {
        finalizeQueueDrainJob = new FinalizeQueueDrainJob();
        ReflectionTestUtils.setField(
                finalizeQueueDrainJob, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("종료 유예시간 뒤 이벤트 Stream의 남은 메시지를 DLQ로 이관한다")
    void executeDrainsRemainingEventStreamMessages() throws Exception {
        givenEventId();
        when(waitingQueueService.drainEventStream(3L, REDIS_EVENT_ISSUE_GROUP)).thenReturn(2L);

        finalizeQueueDrainJob.execute(context);

        verify(waitingQueueService).drainEventStream(3L, REDIS_EVENT_ISSUE_GROUP);
    }

    @Test
    @DisplayName("drain 실패를 Quartz 실패로 전달해 다음 반복 실행 기회를 유지한다")
    void executePropagatesDrainFailure() {
        givenEventId();
        when(waitingQueueService.drainEventStream(3L, REDIS_EVENT_ISSUE_GROUP))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        assertThatThrownBy(() -> finalizeQueueDrainJob.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("eventId: 3");
    }

    @Test
    @DisplayName("Redis가 비활성화된 환경에서는 종료 drain을 건너뛴다")
    void executeSkipsWhenRedisIsDisabled() {
        ReflectionTestUtils.setField(finalizeQueueDrainJob, "waitingQueueService", null);

        assertThatCode(() -> finalizeQueueDrainJob.execute(context)).doesNotThrowAnyException();
    }

    private void givenEventId() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("eventId", 3L);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
    }
}
