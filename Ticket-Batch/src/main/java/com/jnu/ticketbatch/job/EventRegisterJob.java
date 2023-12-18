package com.jnu.ticketbatch.job;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import com.jnu.ticketbatch.config.QuartzJobLauncher;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventRegisterJob implements Job {
    @Autowired private JobLauncher jobLauncher;

    @Autowired private org.springframework.batch.core.Job reserveJob;

    @Autowired private ApplicationContext applicationContext;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobParameters jobParameters =
                    new JobParametersBuilder()
                            .addLong("eventId", context.getMergedJobDataMap().getLong("eventId"))
                            .toJobParameters();

            jobLauncher.run((org.springframework.batch.core.Job) reserveJob, jobParameters);
        } catch (Exception e) {
            throw new JobExecutionException("Failed to execute Spring Batch job", e);
        }
    }

    public void registerJob(Long eventId, LocalDateTime startAt) throws Exception {
        SchedulerFactory schedFact = new StdSchedulerFactory();
        Scheduler sched = schedFact.getScheduler();
        sched.start();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("applicationContext", applicationContext);
        jobDataMap.put("eventId", eventId);
        JobDetail reserveEventQuartzJob =
                newJob(QuartzJobLauncher.class)
                        .withIdentity("RESERVATION_JOB", "group1")
                        .usingJobData("eventId", eventId) // Pass eventId as job data
                        .setJobData(jobDataMap)
                        .build();

        Date date = Date.from(startAt.atZone(ZoneId.of("Asia/Seoul")).toInstant());

        TriggerBuilder<Trigger> triggerTriggerBuilder = newTrigger();
        triggerTriggerBuilder.withIdentity("RESERVATION_TRIGGER", "group1");
        triggerTriggerBuilder.startAt(date);
        triggerTriggerBuilder.forJob(reserveEventQuartzJob);
        Trigger reserveTrigger = triggerTriggerBuilder.build();

        sched.scheduleJob(reserveEventQuartzJob, reserveTrigger);
    }
}
