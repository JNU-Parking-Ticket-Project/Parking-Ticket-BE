package com.jnu.ticketapi.common.aop;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_FAILURE_STORE;
import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_STORE;

import com.jnu.ticketinfrastructure.domainEvent.EventIssuedEvent;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueFailureHandlingAspect {

    private final WaitingQueueService waitingQueueService;
    private static final Logger tracker = LoggerFactory.getLogger("processTracker");

    @Around("@annotation(queueFailureHandling)")
    public Object handleQueueFailures(
            ProceedingJoinPoint joinPoint, QueueFailureHandling queueFailureHandling)
            throws Throwable {

        // 메서드 파라미터에서 EventIssuedEvent 추출
        EventIssuedEvent event = extractEventFromArgs(joinPoint.getArgs());
        if (event == null) {
            return joinPoint.proceed();
        }

        Long userId = event.getMessage().getUserId();
        String failCountKey = queueFailureHandling.failCountKeyPrefix() + userId;

        try {
            // 실제 비즈니스 로직 실행
            return joinPoint.proceed();

        } catch (Exception e) {
            handleException(e, event, queueFailureHandling, failCountKey);
            return null;
        }
    }

    private void handleException(
            Exception e,
            EventIssuedEvent event,
            QueueFailureHandling annotation,
            String failCountKey) {

        Long userId = event.getMessage().getUserId();
        Class<? extends Throwable> exceptionClass = e.getClass();

        // 1. 즉시 제거 대상 예외 확인 (정상적인 비즈니스 로직)
        if (isImmediateRemovalException(exceptionClass, annotation.removeImmediatelyExceptions())) {
            waitingQueueService.remove(REDIS_EVENT_ISSUE_STORE, event.getMessage());
            tracker.info(
                    "비즈니스 로직 예외, 큐에서 제거 - UserId: {}, 예외: {}",
                    userId,
                    e.getClass().getSimpleName());
            return; // failCount 증가 없이 종료
        }

        // 2. 즉시 제거 대상 예외가 아닌 예외는 failCount 증가
        int failCount = waitingQueueService.incrementFailCount(failCountKey);

        if (failCount > annotation.maxRetries()) {
            waitingQueueService.clearFailCount(failCountKey);
            waitingQueueService.remove(REDIS_EVENT_ISSUE_STORE, event.getMessage());
            waitingQueueService.registerQueue(
                    REDIS_EVENT_FAILURE_STORE, event.getMessage(), event.getScore());
            tracker.error(
                    "재시도 한계 초과 ({}회), 큐에서 제거 및 실패 큐에 저장 - UserId: {}, 예외: {}",
                    failCount,
                    userId,
                    e.getMessage(),
                    e);
        } else {
            tracker.error(
                    "재시도 가능 예외 발생 ({}/{}회) - UserId: {}, 예외타입: {}, 메시지: {}",
                    failCount,
                    annotation.maxRetries(),
                    userId,
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    private boolean isImmediateRemovalException(
            Class<? extends Throwable> exceptionClass,
            Class<? extends Throwable>[] removeExceptions) {
        return Arrays.stream(removeExceptions)
                .anyMatch(remove -> remove.isAssignableFrom(exceptionClass));
    }

    private EventIssuedEvent extractEventFromArgs(Object[] args) {
        return Arrays.stream(args)
                .filter(EventIssuedEvent.class::isInstance)
                .map(EventIssuedEvent.class::cast)
                .findFirst()
                .orElse(null);
    }
}
