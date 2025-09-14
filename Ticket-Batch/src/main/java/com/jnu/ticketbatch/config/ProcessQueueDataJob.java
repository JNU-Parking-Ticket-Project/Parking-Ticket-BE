package com.jnu.ticketbatch.config;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.domainEvent.Events;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.jnu.ticketinfrastructure.service.queue.SectorThreadPoolManager;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

@Slf4j
@DisallowConcurrentExecution
public class ProcessQueueDataJob implements Job {
    @Autowired private ApplicationEventPublisher publisher;

    @Autowired private WaitingQueueService waitingQueueService;

    @Autowired private SectorThreadPoolManager sectorThreadPoolManager;
    private static final Logger tracker = LoggerFactory.getLogger("processTracker");


    private final int batchSize = 10;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {

            // 1. Redis에서 배치로 데이터 가져오기 (제거하지 않음)
            List<TypedTuple<Object>> batch =
                    waitingQueueService.peekBatch(REDIS_EVENT_ISSUE_STORE, batchSize);

            if (batch.isEmpty()) {
                return;
            }

            // 2. 각 메시지를 해당하는 구간의 미리 생성된 스레드풀에 개별 분배
            for (TypedTuple<Object> messageWithScore : batch) {
                ChatMessage message = (ChatMessage) messageWithScore.getValue();
                Long sectorId = message.getSectorId();
                Double score = messageWithScore.getScore();

                // 해당 구간의 미리 생성된 스레드풀에 개별 작업 제출
                sectorThreadPoolManager.submitToSector(
                        sectorId,
                        () -> {
                            // 각 스레드에서 Publisher 설정
                            Events.setPublisher(publisher);
                            Events.raise(EventIssuedEvent.from(message, score));
                        });
            }

            tracker.info("{} 개의 메시지를 처리 대기열에 제출함", batch.size());

        } catch (Exception e) {
            log.error("ProcessQueueDataJob 실행 중 오류 발생", e);
        }
    }
}
