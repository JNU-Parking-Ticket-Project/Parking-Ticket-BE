package com.jnu.ticketbatch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.jnu.ticketbatch.config.FinalizeQueueDrainJob;
import com.jnu.ticketbatch.config.ProcessQueueDataJob;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventRegisterJobTest {

    @Mock private Scheduler scheduler;

    private EventRegisterJob eventRegisterJob;

    @BeforeEach
    void setUp() {
        eventRegisterJob = new EventRegisterJob();
        ReflectionTestUtils.setField(eventRegisterJob, "scheduler", scheduler);
    }

    @Test
    @DisplayName("대기열 소비 종료 60초 뒤 세 번의 종료 drain을 예약한다")
    void processQueueDataJobSchedulesFinalDrainRetries() throws Exception {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 29, 10, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 29, 10, 10);
        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

        eventRegisterJob.processQueueDataJob(3L, startAt, endAt);

        verify(scheduler, times(2)).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
        List<JobDetail> jobs = jobCaptor.getAllValues();
        List<Trigger> triggers = triggerCaptor.getAllValues();
        assertThat(jobs)
                .extracting(JobDetail::getJobClass)
                .containsExactly(ProcessQueueDataJob.class, FinalizeQueueDrainJob.class);

        SimpleTrigger finalizeTrigger = (SimpleTrigger) triggers.get(1);
        Date expectedStart =
                Date.from(
                        endAt.plusMinutes(5)
                                .plusSeconds(60)
                                .atZone(ZoneId.of("Asia/Seoul"))
                                .toInstant());
        assertThat(finalizeTrigger.getStartTime()).isEqualTo(expectedStart);
        assertThat(finalizeTrigger.getRepeatInterval()).isEqualTo(30_000L);
        assertThat(finalizeTrigger.getRepeatCount()).isEqualTo(2);
        verify(scheduler).start();
    }
}
