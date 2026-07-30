package com.jnu.ticketbatch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

@ExtendWith(MockitoExtension.class)
class LegacyProcessQueueJobCleanupTest {

    @Mock private Scheduler scheduler;

    @Test
    void removesOnlyLegacyProcessQueueJobsBeforeQuartzStarts() throws Exception {
        JobKey legacyJob = new JobKey("PROCESS_QUEUE_DATA_JOB5", "group1");
        JobKey openJob = new JobKey("RESERVATION_JOB5", "group1");
        JobKey closeJob = new JobKey("EXPIRED_JOB5", "group1");
        when(scheduler.getJobKeys(GroupMatcher.anyJobGroup()))
                .thenReturn(Set.of(legacyJob, openJob, closeJob));
        LegacyProcessQueueJobCleanup cleanup = new LegacyProcessQueueJobCleanup(scheduler);

        cleanup.start();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JobKey>> jobKeys = ArgumentCaptor.forClass(List.class);
        verify(scheduler).deleteJobs(jobKeys.capture());
        assertThat(jobKeys.getValue()).containsExactly(legacyJob);
        verify(scheduler).start();
        assertThat(cleanup.isRunning()).isTrue();
        assertThat(cleanup.getPhase()).isLessThan(Integer.MAX_VALUE);
    }

    @Test
    void doesNothingWhenLegacyProcessQueueJobDoesNotExist() throws Exception {
        when(scheduler.getJobKeys(GroupMatcher.anyJobGroup()))
                .thenReturn(Set.of(new JobKey("EXPIRED_JOB5", "group1")));
        LegacyProcessQueueJobCleanup cleanup = new LegacyProcessQueueJobCleanup(scheduler);

        cleanup.start();

        verify(scheduler, never()).deleteJobs(anyList());
        verify(scheduler).start();
        assertThat(cleanup.isRunning()).isTrue();
    }

    @Test
    void failsStartupWhenLegacyJobCleanupCannotInspectJobStore() throws Exception {
        when(scheduler.getJobKeys(GroupMatcher.anyJobGroup()))
                .thenThrow(new SchedulerException("job store unavailable"));
        LegacyProcessQueueJobCleanup cleanup = new LegacyProcessQueueJobCleanup(scheduler);

        assertThatThrownBy(cleanup::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROCESS_QUEUE_DATA_JOB");
        verify(scheduler, never()).start();
        assertThat(cleanup.isRunning()).isFalse();
    }

    @Test
    void failsStartupWhenQuartzCannotStartAfterCleanup() throws Exception {
        when(scheduler.getJobKeys(GroupMatcher.anyJobGroup())).thenReturn(Set.of());
        org.mockito.Mockito.doThrow(new SchedulerException("scheduler unavailable"))
                .when(scheduler)
                .start();
        LegacyProcessQueueJobCleanup cleanup = new LegacyProcessQueueJobCleanup(scheduler);

        assertThatThrownBy(cleanup::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("start Quartz");
        assertThat(cleanup.isRunning()).isFalse();
    }
}
