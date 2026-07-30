package com.jnu.ticketbatch.config;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/** Removes durable 400ms polling jobs from JDBC JobStore, then starts Quartz recovery. */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyProcessQueueJobCleanup implements SmartLifecycle {

    static final String LEGACY_JOB_NAME_PREFIX = "PROCESS_QUEUE_DATA_JOB";
    private static final int PHASE_BEFORE_QUARTZ_START = Integer.MAX_VALUE - 1;

    private final Scheduler scheduler;
    private volatile boolean running;

    @Override
    public void start() {
        if (running) {
            return;
        }

        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
            List<JobKey> legacyJobKeys =
                    jobKeys.stream()
                            .filter(jobKey -> jobKey.getName().startsWith(LEGACY_JOB_NAME_PREFIX))
                            .toList();

            if (!legacyJobKeys.isEmpty()) {
                scheduler.deleteJobs(legacyJobKeys);
                log.info(
                        "Removed legacy Quartz polling jobs before scheduler startup. jobs: {}",
                        legacyJobKeys);
            }
            scheduler.start();
            running = true;
        } catch (SchedulerException e) {
            throw new IllegalStateException(
                    "Failed to remove legacy PROCESS_QUEUE_DATA_JOB entries and start Quartz", e);
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE_BEFORE_QUARTZ_START;
    }
}
