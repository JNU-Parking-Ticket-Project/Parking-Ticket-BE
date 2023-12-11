package com.jnu.ticketbatch.config;


import java.util.Date;
import lombok.SneakyThrows;
import org.quartz.JobExecutionContext;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class QuartzJobLauncher extends QuartzJobBean {

    @Autowired private Job eventExpirationJob;

    @Autowired private JobLauncher jobLauncher;

    @SneakyThrows
    @Override
    @Scheduled(cron = "0 0/1 * * * ?")
    protected void executeInternal(JobExecutionContext context) {
        JobParameters jobParameters =
                new JobParametersBuilder().addLong("id", new Date().getTime()).toJobParameters();
        jobLauncher.run(eventExpirationJob, jobParameters);
    }
}
