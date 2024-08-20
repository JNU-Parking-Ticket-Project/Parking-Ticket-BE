package com.jnu.ticketbatch.config;

import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.domainEvent.Events;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.ChatMessageStatus;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;


@Slf4j
@DisallowConcurrentExecution
public class ProcessQueueDataJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info(">>>>>>>>>>>>> ProcessQueueDataJob execute");

        ApplicationContext applicationContext =
                (ApplicationContext) context.getJobDetail().getJobDataMap().get("applicationContext");
        PlatformTransactionManager transactionManager = applicationContext.getBean(PlatformTransactionManager.class);

        JobDataMap jobDataMap = context.getMergedJobDataMap();
        ApplicationEventPublisher publisher = (ApplicationEventPublisher) jobDataMap.get("applicationEventPublisher");

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            // 현재 쓰레드에 ApplicationEventPublisher를 설정
            Events.setPublisher(publisher);

            WaitingQueueService waitingQueueService = applicationContext.getBean(WaitingQueueService.class);
            ChatMessage message = waitingQueueService.getValue(REDIS_EVENT_ISSUE_STORE);
            // 메세지가 있으면 이벤트를 publish 하고 redis에 데이터를 재등록 한다.(Waiting 상태로)
            if (message != null) {
                log.info("Message found, raising event");
                Double score = waitingQueueService.getScore(REDIS_EVENT_ISSUE_STORE, message);
                message.setStatus(ChatMessageStatus.WAITING.name());
                log.info("score: {}", score);
                waitingQueueService.reRegisterQueue(REDIS_EVENT_ISSUE_STORE, message, score);
                Events.raise(EventIssuedEvent.from(message.getSectorId(), message.getUserId(), message.getEventId(), message.getRegistration(), score));
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            log.error("ProcessQueueDataJob Exception: {}", e.getMessage());
            transactionManager.rollback(status);
        }
    }
}