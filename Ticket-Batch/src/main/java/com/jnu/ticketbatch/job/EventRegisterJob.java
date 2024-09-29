package com.jnu.ticketbatch.job;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import com.jnu.ticketbatch.config.ProcessQueueDataJob;
import com.jnu.ticketbatch.config.QuartzJobLauncher;
import com.jnu.ticketbatch.expired.BatchQuartzJob;
import com.jnu.ticketdomain.domains.events.EventExpiredEventRaiseGateway;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventRegisterJob implements Job {

    @Autowired private JobLauncher jobLauncher;
    @Autowired private org.springframework.batch.core.Job expirationJob;
    @Autowired private EventExpiredEventRaiseGateway eventExpiredEventRaiseGateway;
    @Autowired private ApplicationEventPublisher applicationEventPublisher;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Autowired private Scheduler scheduler;

    private static final String EVENT_ID = "eventId";
    private static final String GROUP = "group1";
    private static final String ASIA_SEOUL = "Asia/Seoul";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobParameters jobParameters =
                    new JobParametersBuilder()
                            .addLong(EVENT_ID, context.getMergedJobDataMap().getLong(EVENT_ID))
                            .toJobParameters();

            jobLauncher.run(expirationJob, jobParameters);
        } catch (Exception e) {
            throw new JobExecutionException("Failed to execute Spring Batch job", e);
        }
    }

    public void registerJob(Long eventId, LocalDateTime startAt) throws Exception {
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(EVENT_ID, eventId);
        JobDetail reserveEventQuartzJob =
                newJob(QuartzJobLauncher.class)
                        .withIdentity("RESERVATION_JOB" + eventId, GROUP)
                        .usingJobData(EVENT_ID, eventId) // Pass eventId as job data
                        .setJobData(jobDataMap)
                        .build();

        Date date = Date.from(startAt.atZone(ZoneId.of(ASIA_SEOUL)).toInstant());

        Trigger reserveTrigger =
                newTrigger()
                        .withIdentity("RESERVATION_TRIGGER" + eventId, GROUP)
                        .startAt(date)
                        .forJob(reserveEventQuartzJob)
                        .build();

        log.info(">>>>> Event OPEN 스케줄링 등록");

        scheduler.scheduleJob(reserveEventQuartzJob, reserveTrigger);
    }

    public void expiredJob(Long eventId, LocalDateTime endAt) throws SchedulerException {
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(EVENT_ID, eventId);
        JobDetail expiredEventQuartzJob =
                newJob(BatchQuartzJob.class)
                        .withIdentity("EXPIRED_JOB" + eventId, GROUP)
                        .usingJobData(jobDataMap) //                .usingJobData("endAt",
                        // endAt.toString())
                        .build();

        Date date = Date.from(endAt.atZone(ZoneId.of(ASIA_SEOUL)).toInstant());

        Trigger reserveTrigger =
                newTrigger()
                        .withIdentity("EXPIRED_TRIGGER" + eventId, GROUP)
                        .startAt(date)
                        .forJob(expiredEventQuartzJob)
                        .build();

        log.info(">>>>> Event 만료 스케줄링 등록");

        scheduler.scheduleJob(expiredEventQuartzJob, reserveTrigger);
    }

    public void processQueueDataJob(Long eventId, LocalDateTime startAt, LocalDateTime endAt)
            throws Exception {
        scheduler.start();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(EVENT_ID, eventId);

        JobDetail processQueueDataJob =
                newJob(ProcessQueueDataJob.class)
                        .withIdentity("PROCESS_QUEUE_DATA_JOB" + eventId, GROUP)
                        .usingJobData(jobDataMap) // eventId만 JobDataMap에 추가
                        .build();

        Date start = Date.from(startAt.atZone(ZoneId.of(ASIA_SEOUL)).toInstant());
        Date end = Date.from(endAt.atZone(ZoneId.of(ASIA_SEOUL)).toInstant());

        Trigger reserveTrigger =
                newTrigger()
                        .withIdentity("PROCESS_QUEUE_DATA_TRIGGER" + eventId, GROUP)
                        .startAt(start)
                        .endAt(end)
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule()
                                        .withIntervalInMilliseconds(400)
                                        .repeatForever())
                        .forJob(processQueueDataJob)
                        .build();

        log.info(">>>>> ProcessQueueData 스케줄링 등록");

        scheduler.scheduleJob(processQueueDataJob, reserveTrigger);
    }
}
