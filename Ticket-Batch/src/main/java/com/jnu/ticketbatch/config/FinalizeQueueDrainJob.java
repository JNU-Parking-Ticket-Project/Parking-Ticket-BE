package com.jnu.ticketbatch.config;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;

import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DisallowConcurrentExecution
public class FinalizeQueueDrainJob implements Job {

    private static final String EVENT_ID = "eventId";

    @Autowired(required = false)
    private WaitingQueueService waitingQueueService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (waitingQueueService == null) {
            log.info("Redis is disabled. Queue drain finalization is skipped.");
            return;
        }

        Long eventId = context.getMergedJobDataMap().getLong(EVENT_ID);
        try {
            long movedCount =
                    waitingQueueService.drainEventStream(eventId, REDIS_EVENT_ISSUE_GROUP);
            if (movedCount > 0) {
                log.warn(
                        "Moved remaining event Stream messages to DLQ. eventId: {}, count: {}",
                        eventId,
                        movedCount);
                return;
            }
            log.info(
                    "Event Stream drain completed without remaining messages. eventId: {}",
                    eventId);
        } catch (Exception e) {
            throw new JobExecutionException(
                    "Failed to finalize Redis Stream drain. eventId: " + eventId, e);
        }
    }
}
