package com.jnu.ticketbatch.job;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import com.jnu.ticketbatch.config.ProcessQueueDataJob;
import com.jnu.ticketbatch.config.QuartzJobLauncher;
import com.jnu.ticketbatch.expired.BatchQuartzJob;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventRegisterJob implements Job {
    @Autowired private JobLauncher jobLauncher;

    @Autowired private org.springframework.batch.core.Job reserveJob;

    @Autowired private ApplicationContext applicationContext;
    @Autowired private StepBuilderFactory stepBuilderFactory;
    @Autowired private JobBuilderFactory jobBuilderFactory;
    @Autowired private ApplicationEventPublisher applicationEventPublisher;
    @Autowired private WaitingQueueService waitingQueueService;

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
        log.info(">>>>> Event OPEN 스케줄링 등록");

        sched.scheduleJob(reserveEventQuartzJob, reserveTrigger);
    }

    public void expiredJob(Long eventId, LocalDateTime endAt) throws SchedulerException {
        // Quartz 스케줄러 초기화
        SchedulerFactory schedFact = new StdSchedulerFactory();
        Scheduler sched = schedFact.getScheduler();
        sched.start();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("applicationContext", applicationContext);
        jobDataMap.put("eventId", eventId);
        jobDataMap.put("dataSource", applicationContext.getBean("dataSource"));
        jobDataMap.put("stepBuilderFactory", stepBuilderFactory);
        jobDataMap.put("jobBuilderFactory", jobBuilderFactory);
        jobDataMap.put("jobLauncher", jobLauncher);
        JobDetail expiredEventQuartzJob =
                newJob(BatchQuartzJob.class)
                        .withIdentity("EXPIRED_JOB", "group1")
                        .usingJobData("eventId", eventId)
                        //                .usingJobData("endAt", endAt.toString())
                        .setJobData(jobDataMap)
                        .build();
        Date date = Date.from(endAt.atZone(ZoneId.of("Asia/Seoul")).toInstant());

        TriggerBuilder<Trigger> triggerTriggerBuilder = newTrigger();
        triggerTriggerBuilder.withIdentity("EXPIRED_TRIGGER", "group1");
        triggerTriggerBuilder.startAt(date);
        triggerTriggerBuilder.forJob(expiredEventQuartzJob);
        Trigger reserveTrigger = triggerTriggerBuilder.build();

        log.info(">>>>> Event 만료 스케줄링 등록");
        sched.scheduleJob(expiredEventQuartzJob, reserveTrigger);
    }

    public void ProcessQueueDataJob(Long eventId, LocalDateTime endAt) throws Exception {
        SchedulerFactory schedFact = new StdSchedulerFactory();
        Scheduler sched = schedFact.getScheduler();
        sched.start();

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("eventId", eventId);
        jobDataMap.put("applicationContext", applicationContext);
        jobDataMap.put("applicationEventPublisher", applicationEventPublisher);
        jobDataMap.put("waitingQueueService", waitingQueueService);

        JobDetail processQueueDataJob =
                newJob(ProcessQueueDataJob.class)
                        .withIdentity("PROCESS_QUEUE_DATA_JOB", "group1")
                        .usingJobData("eventId", eventId)
                        .setJobData(jobDataMap)
                        .build();
        Date end = Date.from(endAt.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        Date start =
                Date.from(
                        LocalDateTime.now()
                                .plusMinutes(1)
                                .atZone(ZoneId.of("Asia/Seoul"))
                                .toInstant());
        TriggerBuilder<Trigger> triggerTriggerBuilder = newTrigger();
        triggerTriggerBuilder.withIdentity("PROCESS_QUEUE_DATA_TRIGGER", "group1");
        triggerTriggerBuilder.startAt(start);
        triggerTriggerBuilder.endAt(end);
        triggerTriggerBuilder.withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(1)
                        .repeatForever()); // 1초마다 실행;
        triggerTriggerBuilder.forJob(processQueueDataJob);
        Trigger reserveTrigger = triggerTriggerBuilder.build();

        log.info(">>>>> ProcessQueueData 스케줄링 등록");
        sched.scheduleJob(processQueueDataJob, reserveTrigger);
    }
}
