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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.TimeUnit;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;


@Slf4j
@DisallowConcurrentExecution
public class ProcessQueueDataJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info(">>>>>>>>>>>>> ProcessQueueDataJob execute");
        log.info("Thread: {}", Thread.currentThread().getName());

        ApplicationContext applicationContext =
                (ApplicationContext) context.getJobDetail().getJobDataMap().get("applicationContext");
        PlatformTransactionManager transactionManager = applicationContext.getBean(PlatformTransactionManager.class);
        StringRedisTemplate stringRedisTemplate = applicationContext.getBean(StringRedisTemplate.class);

        JobDataMap jobDataMap = context.getMergedJobDataMap();
        ApplicationEventPublisher publisher = (ApplicationEventPublisher) jobDataMap.get("applicationEventPublisher");
        try {
            // 현재 쓰레드에 ApplicationEventPublisher를 설정
            Events.setPublisher(publisher);

            WaitingQueueService waitingQueueService = (WaitingQueueService) jobDataMap.get("waitingQueueService");
            ChatMessage message = waitingQueueService.getValueNotWaiting(REDIS_EVENT_ISSUE_STORE);

            if (message != null) {
                String lockKey = "lock:" + message.getEventId();
                ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

                // 락 획득
                boolean acquired = ops.setIfAbsent(lockKey, "lock", 1, TimeUnit.SECONDS);

                if (acquired) {
                    try {
                        log.info("Message found, raising event");
                        Double score = waitingQueueService.getScore(REDIS_EVENT_ISSUE_STORE, message);
                        waitingQueueService.reRegisterQueue(REDIS_EVENT_ISSUE_STORE,message ,ChatMessageStatus.WAITING, score);
                        Events.raise(EventIssuedEvent.from(message.getSectorId(), message.getUserId(), message.getEventId(), message.getRegistration(), score));
                    } finally {
                        // 락 해제
                        stringRedisTemplate.delete(lockKey);
                        log.info("Lock released for event: {}", message.getEventId());
                    }
                } else {
                    log.info("Lock not acquired, another process might be handling the event.");
                }
            }
        } catch (Exception e) {
            log.error("ProcessQueueDataJob Exception: {}", e.getMessage());
        }
    }
}