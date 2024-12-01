package com.jnu.ticketbatch.config;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.domainEvent.Events;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

@Slf4j
@DisallowConcurrentExecution
public class ProcessQueueDataJob implements Job {
    @Autowired private ApplicationEventPublisher publisher;

    @Autowired private WaitingQueueService waitingQueueService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            // 현재 쓰레드에 ApplicationEventPublisher를 설정
            Events.setPublisher(publisher);

            Set<TypedTuple<Object>> messagesWithScores =
                    waitingQueueService.findAllWithScore(REDIS_EVENT_ISSUE_STORE);

            if (!messagesWithScores.isEmpty()) {
                for (TypedTuple<Object> messageWithScore : messagesWithScores) {
                    Double score = messageWithScore.getScore();
                    ChatMessage message = (ChatMessage) messageWithScore.getValue();
                    Events.raise(EventIssuedEvent.from(message, score));
                }
            }
        } catch (Exception e) {
            log.error("ProcessQueueDataJob Exception: {}", e.getMessage());
        }
    }
}
