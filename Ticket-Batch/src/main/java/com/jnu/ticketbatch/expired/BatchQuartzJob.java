package com.jnu.ticketbatch.expired;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
public class BatchQuartzJob extends QuartzJobBean {

    private JobLauncher jobLauncher;
    private Job job;
    private JobExplorer jobExplorer;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        ApplicationContext applicationContext =
                (ApplicationContext)
                        context.getJobDetail().getJobDataMap().get("applicationContext");

        // JobLauncher, Job, JobExplorer를 ApplicationContext에서 가져옵니다.
        this.jobLauncher = applicationContext.getBean(JobLauncher.class);
        this.job = applicationContext.getBean(Job.class);
        this.jobExplorer = applicationContext.getBean(JobExplorer.class);
        EventAdaptor eventAdaptor = applicationContext.getBean(EventAdaptor.class);
        RegistrationAdaptor registrationAdaptor =
                applicationContext.getBean(RegistrationAdaptor.class);
        // JobDataMap에서 eventId를 가져옵니다.
        Long eventId = (Long) context.getJobDetail().getJobDataMap().get("eventId");
        Event event = eventAdaptor.findById(eventId);
        eventAdaptor.updateEventStatus(event, EventStatus.CLOSED);

        JobParameters jobParameters =
                new JobParametersBuilder(this.jobExplorer)
                        .getNextJobParameters(this.job)
                        .addLong("eventId", eventId)
                        .toJobParameters();

        try {
            this.jobLauncher.run(this.job, jobParameters);
        } catch (Exception e) {
            log.error("Failed to run batch job", e);
            throw new JobExecutionException(e);
        }
    }
}
