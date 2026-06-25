package com.jnu.ticketbatch.config;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_CONSUMER;
import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;
import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STREAM;

import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.domainEvent.Events;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@DisallowConcurrentExecution
public class ProcessQueueDataJob implements Job {
    @Autowired private ApplicationEventPublisher publisher;

    @Autowired private WaitingQueueService waitingQueueService;

    private static final long READ_COUNT = 100L;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            // 현재 쓰레드에 ApplicationEventPublisher를 설정
            Events.setPublisher(publisher);

            List<StreamQueueMessage> messages =
                    waitingQueueService.readGroup(
                            REDIS_EVENT_ISSUE_STREAM,
                            REDIS_EVENT_ISSUE_GROUP,
                            REDIS_EVENT_ISSUE_CONSUMER,
                            READ_COUNT);

            if (!messages.isEmpty()) {
                for (StreamQueueMessage streamQueueMessage : messages) {
                    Events.raise(
                            EventIssuedEvent.from(
                                    streamQueueMessage.getMessage(),
                                    streamQueueMessage.getRecordId()));
                }
            }
        } catch (Exception e) {
            log.error("ProcessQueueDataJob Exception: {}", e.getMessage());
        }
    }
}
