package com.jnu.ticketbatch.config;

import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.domainEvent.Events;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.jnu.ticketinfrastructure.service.queue.SectorThreadPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import java.util.List;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

@Slf4j
@DisallowConcurrentExecution
public class ProcessQueueDataJob implements Job {
    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private WaitingQueueService waitingQueueService;

    @Autowired
    private SectorThreadPoolManager sectorThreadPoolManager;

    private final int batchSize = 10;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {

            // 1. Redis에서 배치로 데이터 가져오기 (제거하지 않음)
            List<TypedTuple<Object>> batch = 
                    waitingQueueService.peekBatch(REDIS_EVENT_ISSUE_STORE, batchSize);

            if (batch.isEmpty()) {
                log.info("No messages in the queue to process.");
                return;
            }

            log.info("Found {} messages to process", batch.size());

            // 2. 각 메시지를 해당하는 구간의 미리 생성된 스레드풀에 개별 분배
            for (TypedTuple<Object> messageWithScore : batch) {
                ChatMessage message = (ChatMessage) messageWithScore.getValue();
                Long sectorId = message.getSectorId();
                Double score = messageWithScore.getScore();

                // 해당 구간의 미리 생성된 스레드풀에 개별 작업 제출
                sectorThreadPoolManager.submitToSector(sectorId, () -> {
                    // 각 스레드에서 Publisher 설정
                    Events.setPublisher(publisher);
                    Events.raise(EventIssuedEvent.from(message, score));
                });
            }

            log.info("All {} messages distributed to sector thread pools", batch.size());

        } catch (Exception e) {
            log.error("ProcessQueueDataJob Exception: {}", e.getMessage(), e);
        }
    }
}

