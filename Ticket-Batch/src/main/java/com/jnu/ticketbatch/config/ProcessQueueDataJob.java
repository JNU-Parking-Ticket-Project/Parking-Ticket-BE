package com.jnu.ticketbatch.config;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.domainEvent.Events;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.ChatMessageStatus;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@DisallowConcurrentExecution
public class ProcessQueueDataJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info(">>>>>>>>>>>>> ProcessQueueDataJob execute");
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        ApplicationEventPublisher publisher =
                (ApplicationEventPublisher) jobDataMap.get("applicationEventPublisher");
        try {
            // 현재 쓰레드에 ApplicationEventPublisher를 설정
            Events.setPublisher(publisher);

            WaitingQueueService waitingQueueService =
                    (WaitingQueueService) jobDataMap.get("waitingQueueService");
            ChatMessage message =
                    waitingQueueService.getValueByStatus(
                            REDIS_EVENT_ISSUE_STORE, ChatMessageStatus.NOT_WAITING);

            if (message != null) {
                log.info("Message found, raising event");
                Double score = waitingQueueService.getScore(REDIS_EVENT_ISSUE_STORE, message);
                waitingQueueService.reRegisterQueue(
                        REDIS_EVENT_ISSUE_STORE, message, ChatMessageStatus.WAITING, score);
                Events.raise(
                        EventIssuedEvent.from(
                                message.getSectorId(),
                                message.getUserId(),
                                message.getEventId(),
                                message.getRegistration(),
                                score));
            }
        } catch (Exception e) {
            log.error("ProcessQueueDataJob Exception: {}", e.getMessage());
        }
    }
}
