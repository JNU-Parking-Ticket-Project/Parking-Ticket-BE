package com.jnu.ticketbatch.expired;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketdomain.domains.events.EventExpiredEventRaiseGateway;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketinfrastructure.redis.RedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
public class BatchQuartzJob extends QuartzJobBean {

    @Autowired private JobLauncher jobLauncher;
    @Autowired private Job job;
    @Autowired private JobExplorer jobExplorer;

    @Autowired EventAdaptor eventAdaptor;
    @Autowired RedisRepository redisRepository;
    @Autowired RegistrationAdaptor registrationAdaptor;
    @Autowired EventExpiredEventRaiseGateway eventExpiredEventRaiseGateway;


    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // JobDataMap에서 eventId를 가져옵니다.
        Long eventId = (Long) context.getJobDetail().getJobDataMap().get("eventId");
        Event event = eventAdaptor.findById(eventId);
        redisRepository.delete(REDIS_EVENT_ISSUE_STORE);
        eventAdaptor.updateEventStatus(event, EventStatus.CLOSED);

        JobParameters jobParameters =
                new JobParametersBuilder(this.jobExplorer)
                        .getNextJobParameters(this.job)
                        .addLong("eventId", eventId)
                        .toJobParameters();
        log.info("EventThrow in BatchQuartzJob");
        eventExpiredEventRaiseGateway.handle(eventId);
        try {
            this.jobLauncher.run(this.job, jobParameters);
        } catch (Exception e) {
            log.error("Failed to run batch job", e);
            throw new JobExecutionException(e);
        }
    }
}
