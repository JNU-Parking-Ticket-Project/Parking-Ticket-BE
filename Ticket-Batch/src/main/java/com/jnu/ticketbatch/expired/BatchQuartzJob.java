package com.jnu.ticketbatch.expired;


import com.jnu.ticketdomain.domains.events.EventExpiredEventRaiseGateway;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
public class BatchQuartzJob extends QuartzJobBean {

    private static final Duration REDIS_STOCK_DRAIN_TIMEOUT = Duration.ofMinutes(5);

    @Autowired private JobLauncher jobLauncher;
    @Autowired private Job job;
    @Autowired private JobExplorer jobExplorer;

    @Autowired EventAdaptor eventAdaptor;
    @Autowired SectorAdaptor sectorAdaptor;

    @Autowired(required = false)
    WaitingQueueService waitingQueueService;

    @Autowired RegistrationAdaptor registrationAdaptor;
    @Autowired EventExpiredEventRaiseGateway eventExpiredEventRaiseGateway;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // JobDataMap에서 eventId를 가져옵니다.
        Long eventId = (Long) context.getJobDetail().getJobDataMap().get("eventId");
        Event event = eventAdaptor.findById(eventId);
        eventAdaptor.updateEventStatus(event, EventStatus.CLOSED);
        syncAndExpireRedisStock(eventId);

        JobParameters jobParameters =
                new JobParametersBuilder(this.jobExplorer)
                        .getNextJobParameters(this.job)
                        .addLong("eventId", eventId)
                        .toJobParameters();
        log.info("EventThrow in BatchQuartzJob");
        //        eventExpiredEventRaiseGateway.handle(eventId);
        try {
            this.jobLauncher.run(this.job, jobParameters);
        } catch (Exception e) {
            log.error("Failed to run batch job", e);
            throw new JobExecutionException(e);
        }
    }

    void syncAndExpireRedisStock(Long eventId) {
        if (waitingQueueService == null) {
            return;
        }
        waitingQueueService.markEventStockClosed(eventId, REDIS_STOCK_DRAIN_TIMEOUT);
        for (Sector sector : sectorAdaptor.findByEventId(eventId)) {
            waitingQueueService
                    .findRemainingStock(eventId, sector.getId())
                    .ifPresent(
                            remainingAmount -> {
                                sector.syncRemainingAmount(remainingAmount);
                                sectorAdaptor.save(sector);
                            });
        }
        waitingQueueService.expireEventStockKeys(eventId, REDIS_STOCK_DRAIN_TIMEOUT);
    }
}
