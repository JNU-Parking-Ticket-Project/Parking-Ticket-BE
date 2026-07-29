package com.jnu.ticketbatch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.jnu.ticketbatch.config.FinalizeQueueDrainJob;
import com.jnu.ticketbatch.expired.BatchQuartzJob;
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
    @DisplayName("이벤트 종료 Job과 함께 유예시간 이후 세 번의 최종 drain을 예약한다")
    void expiredJobSchedulesFinalDrainRetries() throws Exception {
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 29, 10, 10);
        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

        eventRegisterJob.expiredJob(3L, endAt);

        verify(scheduler, times(2)).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());
        List<JobDetail> jobs = jobCaptor.getAllValues();
        List<Trigger> triggers = triggerCaptor.getAllValues();
        assertThat(jobs)
                .extracting(JobDetail::getJobClass)
                .containsExactly(BatchQuartzJob.class, FinalizeQueueDrainJob.class);

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
