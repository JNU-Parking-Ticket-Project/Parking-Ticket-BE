package com.jnu.ticketbatch.config;


import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketinfrastructure.admission.RegistrationAdmissionFallbackGateway;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.jnu.ticketinfrastructure.stream.RedisStreamConsumerManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

@RequiredArgsConstructor
public class QuartzJobLauncher implements Job {
    @Autowired private EventAdaptor eventAdaptor;
    @Autowired private SectorAdaptor sectorAdaptor;

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Autowired(required = false)
    private RedisStreamConsumerManager streamConsumerManager;

    @Autowired(required = false)
    private RegistrationAdmissionFallbackGateway admissionFallbackGateway;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap jobDataMap = context.getMergedJobDataMap();
            Long eventId = jobDataMap.getLong("eventId");
            Event event = eventAdaptor.findById(eventId);
            if (waitingQueueService != null) {
                List<Sector> sectors = sectorAdaptor.findByEventId(eventId);
                try {
                    waitingQueueService.initializeEventStock(eventId, sectors);
                } catch (DataAccessException exception) {
                    activateFallback(eventId, exception);
                }
            } else {
                activateFallback(eventId, null);
            }
            eventAdaptor.updateEventStatus(event, EventStatus.OPEN);
            if (streamConsumerManager != null) {
                streamConsumerManager.start(eventId);
            }
        } catch (Exception e) {
            throw new JobExecutionException("Failed to initialize and open event", e);
        }
    }

    private void activateFallback(Long eventId, Throwable cause) {
        if (admissionFallbackGateway == null) {
            throw new IllegalStateException(
                    "Redis admission failed and DB fallback is unavailable. eventId=" + eventId,
                    cause);
        }
        admissionFallbackGateway.activateDatabaseFallback(eventId, cause);
    }
}
