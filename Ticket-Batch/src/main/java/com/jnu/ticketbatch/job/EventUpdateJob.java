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
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
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
import org.springframework.retry.annotation.Retryable;

@Configuration
@Slf4j
public class EventUpdateJob implements Job {

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

            jobLauncher.run(reserveJob, jobParameters);
        } catch (Exception e) {
            throw new JobExecutionException("Failed to execute Spring Batch job", e);
        }
    }

    @Retryable(value = SchedulerException.class, maxAttempts = 3)
    public void cancelScheduledJob(Long eventId) {
        try {
            SchedulerFactory schedFact = new StdSchedulerFactory();
            Scheduler sched = schedFact.getScheduler();

            // JobKey 생성
            JobKey jobKey1 = JobKey.jobKey("RESERVATION_JOB", "group1");

            JobKey jobKey2 = JobKey.jobKey("EXPIRED_JOB", "group1");
            // 스케줄러에서 작업 삭제
            if (sched.checkExists(jobKey1)) { // 해당 JobKey로 등록된 작업이 존재하는지 확인
                sched.deleteJob(jobKey1); // 작업 삭제
                log.info(">>>>> 예약 생성 작업 스케줄러에서 삭제");
            }
            if (sched.checkExists(jobKey2)) { // 해당 JobKey로 등록된 작업이 존재하는지 확인
                sched.deleteJob(jobKey2); // 작업 삭제
                log.info(">>>>> 만료 작업 스케줄러에서 삭제");
            }
        } catch (SchedulerException e) {
            log.error(">>>>> 예약 생성 작업 스케줄러에서 삭제 실패" + e.getMessage());
        }
    }

    public void reRegisterJob(Long eventId, LocalDateTime startAt) throws Exception {
        // Quartz 스케줄러 초기화
        SchedulerFactory schedFact = new StdSchedulerFactory();
        Scheduler sched = schedFact.getScheduler();
        sched.start();
        // 예약 생성 작업 정의
        cancelScheduledJob(eventId);

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
