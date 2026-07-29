package com.jnu.ticketbatch.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventUpdateJobTest {

    @Mock private Scheduler scheduler;

    private EventUpdateJob eventUpdateJob;

    @BeforeEach
    void setUp() {
        eventUpdateJob = new EventUpdateJob();
        ReflectionTestUtils.setField(eventUpdateJob, "sched", scheduler);
    }

    @Test
    @DisplayName("이벤트 일정 변경 시 기존 종료 drain Job도 함께 취소한다")
    void cancelScheduledJobDeletesFinalDrainJob() throws Exception {
        JobKey finalizeJobKey = JobKey.jobKey("FINALIZE_QUEUE_DRAIN_JOB3", "group1");
        when(scheduler.checkExists(any(JobKey.class)))
                .thenAnswer(invocation -> finalizeJobKey.equals(invocation.getArgument(0)));

        eventUpdateJob.cancelScheduledJob(3L);

        verify(scheduler).deleteJob(finalizeJobKey);
    }
}
