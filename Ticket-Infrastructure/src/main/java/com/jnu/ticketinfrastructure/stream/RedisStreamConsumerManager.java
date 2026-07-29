package com.jnu.ticketinfrastructure.stream;

import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_CONSUMER;
import static com.jnu.ticketcommon.consts.TicketStatic.REDIS_EVENT_ISSUE_GROUP;

import com.jnu.ticketinfrastructure.model.AutoClaimResult;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
import com.jnu.ticketinfrastructure.model.StreamConsumerState;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnExpression("${ableRedis:true}")
public class RedisStreamConsumerManager {

    private static final String INITIAL_CLAIM_ID = "0-0";
    private static final long ERROR_BACKOFF_MILLIS = 500L;

    private final WaitingQueueService waitingQueueService;
    private final RegistrationStreamMessageHandler messageHandler;
    private final Duration pollTimeout;
    private final Duration pendingMinIdleTime;
    private final Duration pendingClaimInterval;
    private final Duration drainQuietPeriod;
    private final int batchSize;
    private final int maxProcessingFailures;
    private final String consumerInstanceId;
    private final ExecutorService coordinatorExecutor;
    private final ThreadPoolExecutor processingExecutor;
    private final Map<Long, EventConsumer> consumers = new ConcurrentHashMap<>();
    private final AtomicBoolean shuttingDown = new AtomicBoolean();

    public RedisStreamConsumerManager(
            WaitingQueueService waitingQueueService,
            ObjectProvider<RegistrationStreamMessageHandler> messageHandlerProvider,
            @Value("${spring.datasource.hikari.maximum-pool-size:20}") int dbPoolSize,
            @Value("${redis.stream.consumer.db-pool-reserve:5}") int dbPoolReserve,
            @Value("${redis.stream.consumer.max-concurrency:15}") int configuredConcurrency,
            @Value("${redis.stream.consumer.queue-capacity:30}") int queueCapacity,
            @Value("${redis.stream.consumer.max-active-events:4}") int maxActiveEvents,
            @Value("${redis.stream.consumer.batch-size:50}") int batchSize,
            @Value("${redis.stream.consumer.poll-timeout-ms:500}") long pollTimeoutMillis,
            @Value("${redis.stream.consumer.pending-min-idle-ms:30000}") long pendingMinIdleMillis,
            @Value("${redis.stream.consumer.pending-claim-interval-ms:5000}")
                    long pendingClaimIntervalMillis,
            @Value("${redis.stream.consumer.max-processing-failures:3}")
                    int maxProcessingFailures,
            @Value("${redis.stream.consumer.drain-quiet-period-ms:5000}")
                    long drainQuietPeriodMillis) {
        this.waitingQueueService = waitingQueueService;
        this.messageHandler = messageHandlerProvider.getIfAvailable();
        this.batchSize = Math.max(1, batchSize);
        this.pollTimeout = Duration.ofMillis(Math.max(1L, pollTimeoutMillis));
        this.pendingMinIdleTime = Duration.ofMillis(Math.max(1L, pendingMinIdleMillis));
        this.pendingClaimInterval = Duration.ofMillis(Math.max(1L, pendingClaimIntervalMillis));
        this.maxProcessingFailures = Math.max(1, maxProcessingFailures);
        this.drainQuietPeriod = Duration.ofMillis(Math.max(0L, drainQuietPeriodMillis));
        this.consumerInstanceId = UUID.randomUUID().toString().substring(0, 8);

        int availableDbConnections = Math.max(1, dbPoolSize - Math.max(0, dbPoolReserve));
        int processingConcurrency =
                Math.max(1, Math.min(configuredConcurrency, availableDbConnections));
        int boundedQueueCapacity = Math.max(1, queueCapacity);
        this.processingExecutor =
                new ThreadPoolExecutor(
                        processingConcurrency,
                        processingConcurrency,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(boundedQueueCapacity),
                        namedThreadFactory("redis-stream-worker-"),
                        this::blockUntilQueueHasCapacity);
        this.coordinatorExecutor =
                Executors.newFixedThreadPool(
                        Math.max(1, maxActiveEvents), namedThreadFactory("redis-stream-consumer-"));

        log.info(
                "Redis Stream consumer configured. processingConcurrency: {}, queueCapacity: {}, dbPoolSize: {}, dbPoolReserve: {}",
                processingConcurrency,
                boundedQueueCapacity,
                dbPoolSize,
                dbPoolReserve);
    }

    public synchronized boolean start(Long eventId) {
        if (messageHandler == null) {
            log.warn("Redis Stream consumer was not started because no message handler exists");
            return false;
        }
        if (shuttingDown.get()) {
            return false;
        }

        EventConsumer current = consumers.get(eventId);
        if (current != null && current.active.get()) {
            return false;
        }
        EventConsumer consumer = new EventConsumer(eventId, streamKey(eventId));
        consumers.put(eventId, consumer);
        consumer.future = coordinatorExecutor.submit(() -> consume(consumer));
        return true;
    }

    public void requestDrain(Long eventId) {
        EventConsumer consumer = consumers.get(eventId);
        if (consumer == null) {
            return;
        }
        consumer.drainRequested.set(true);
        log.info("Redis Stream drain requested. eventId: {}", eventId);
    }

    public void stopImmediately(Long eventId) {
        EventConsumer consumer = consumers.remove(eventId);
        if (consumer == null) {
            return;
        }
        synchronized (consumer) {
            consumer.active.set(false);
        }
        if (consumer.future != null) {
            consumer.future.cancel(true);
        }
        log.info("Redis Stream subscription stopped. eventId: {}", eventId);
    }

    public boolean pauseForFinalDrain(Long eventId) {
        EventConsumer consumer = consumers.get(eventId);
        if (consumer == null) {
            return true;
        }

        boolean idle;
        synchronized (consumer) {
            consumer.active.set(false);
            idle = consumer.inFlight.get() == 0;
        }
        if (consumer.future != null) {
            consumer.future.cancel(true);
        }
        if (idle) {
            consumers.remove(eventId, consumer);
        }
        log.info(
                "Redis Stream subscription paused for final drain. eventId: {}, inFlight: {}",
                eventId,
                consumer.inFlight.get());
        return idle;
    }

    public boolean isRunning(Long eventId) {
        EventConsumer consumer = consumers.get(eventId);
        return consumer != null && consumer.active.get();
    }

    public StreamConsumerState getState(Long eventId) {
        EventConsumer consumer = consumers.get(eventId);
        if (consumer == null) {
            return new StreamConsumerState(0L, 0L, 0);
        }
        return waitingQueueService.getConsumerState(
                consumer.streamKey, REDIS_EVENT_ISSUE_GROUP, consumer.inFlight.get());
    }

    private void consume(EventConsumer consumer) {
        log.info("Redis Stream subscription started. eventId: {}", consumer.eventId);
        while (consumer.active.get() && !Thread.currentThread().isInterrupted()) {
            try {
                recoverPendingIfDue(consumer);
                List<RawStreamMessage> messages =
                        waitingQueueService.readNewMessages(
                                consumer.streamKey,
                                REDIS_EVENT_ISSUE_GROUP,
                                consumerName(consumer.eventId),
                                batchSize,
                                pollTimeout);
                messages.forEach(message -> dispatch(consumer, message));
                stopWhenDrained(consumer);
            } catch (Exception e) {
                if (consumer.active.get() && !shuttingDown.get()) {
                    log.error("Redis Stream consume failed. eventId: {}", consumer.eventId, e);
                    sleepAfterError();
                }
            }
        }
        removeStoppedConsumerWhenIdle(consumer);
        log.info("Redis Stream subscription ended. eventId: {}", consumer.eventId);
    }

    private void recoverPendingIfDue(EventConsumer consumer) {
        Instant now = Instant.now();
        if (Duration.between(consumer.lastClaimAt, now).compareTo(pendingClaimInterval) < 0) {
            return;
        }
        consumer.lastClaimAt = now;
        AutoClaimResult result =
                waitingQueueService.autoClaimMessages(
                        consumer.streamKey,
                        REDIS_EVENT_ISSUE_GROUP,
                        consumerName(consumer.eventId),
                        batchSize,
                        pendingMinIdleTime,
                        consumer.nextClaimId);
        consumer.nextClaimId = normalizeClaimId(result.nextStartId());
        result.messages().forEach(message -> dispatch(consumer, message));
    }

    private void dispatch(EventConsumer consumer, RawStreamMessage rawMessage) {
        synchronized (consumer) {
            if (!consumer.active.get()
                    || !consumer.inFlightRecordIds.add(rawMessage.getRecordId())) {
                return;
            }
            consumer.inFlight.incrementAndGet();
        }
        try {
            processingExecutor.execute(
                    () -> {
                        try {
                            StreamQueueMessage message =
                                    waitingQueueService.deserialize(consumer.streamKey, rawMessage);
                            messageHandler.handle(message);
                        } catch (Exception e) {
                            recordProcessingFailure(consumer, rawMessage, e);
                        } finally {
                            consumer.inFlight.decrementAndGet();
                            consumer.inFlightRecordIds.remove(rawMessage.getRecordId());
                            stopWhenDrained(consumer);
                            removeStoppedConsumerWhenIdle(consumer);
                        }
                    });
        } catch (RuntimeException e) {
            completeRejectedDispatch(consumer, rawMessage);
            throw e;
        }
    }

    private void recordProcessingFailure(
            EventConsumer consumer, RawStreamMessage rawMessage, Exception exception) {
        try {
            DeadLetterTransferResult result =
                    waitingQueueService.recordProcessingFailure(
                            consumer.streamKey,
                            REDIS_EVENT_ISSUE_GROUP,
                            rawMessage.getRecordId(),
                            rawMessage.getPayload(),
                            maxProcessingFailures,
                            exception);
            if (result.isMoved()) {
                log.error(
                        "Redis Stream message moved to DLQ after {} failed deliveries. eventId: {}, recordId: {}",
                        result.getFailureCount(),
                        consumer.eventId,
                        rawMessage.getRecordId(),
                        exception);
                return;
            }
            log.warn(
                    "Redis Stream message remains Pending after failed delivery {}/{}. eventId: {}, recordId: {}, cause: {}",
                    result.getFailureCount(),
                    maxProcessingFailures,
                    consumer.eventId,
                    rawMessage.getRecordId(),
                    exception.toString());
        } catch (Exception recoveryException) {
            log.error(
                    "Failed to record Redis Stream processing failure. eventId: {}, recordId: {}",
                    consumer.eventId,
                    rawMessage.getRecordId(),
                    recoveryException);
        }
    }

    private void completeRejectedDispatch(EventConsumer consumer, RawStreamMessage rawMessage) {
        consumer.inFlight.decrementAndGet();
        consumer.inFlightRecordIds.remove(rawMessage.getRecordId());
        removeStoppedConsumerWhenIdle(consumer);
    }

    private void removeStoppedConsumerWhenIdle(EventConsumer consumer) {
        if (!consumer.active.get() && consumer.inFlight.get() == 0) {
            consumers.remove(consumer.eventId, consumer);
        }
    }

    private void stopWhenDrained(EventConsumer consumer) {
        if (!consumer.drainRequested.get() || !consumer.active.get()) {
            return;
        }
        StreamConsumerState state =
                waitingQueueService.getConsumerState(
                        consumer.streamKey, REDIS_EVENT_ISSUE_GROUP, consumer.inFlight.get());
        if (!state.isDrained()) {
            consumer.drainedAt = null;
            return;
        }
        Instant now = Instant.now();
        if (consumer.drainedAt == null) {
            consumer.drainedAt = now;
            return;
        }
        if (Duration.between(consumer.drainedAt, now).compareTo(drainQuietPeriod) >= 0) {
            consumer.active.set(false);
            log.info(
                    "Redis Stream drained; subscription will stop. eventId: {}, lag: {}, pending: {}, inFlight: {}",
                    consumer.eventId,
                    state.lag(),
                    state.pending(),
                    state.inFlight());
        }
    }

    private String streamKey(Long eventId) {
        return waitingQueueService.eventStreamKey(eventId);
    }

    private String consumerName(Long eventId) {
        return REDIS_EVENT_ISSUE_CONSUMER + "-" + consumerInstanceId + "-" + eventId;
    }

    private String normalizeClaimId(String nextClaimId) {
        if (nextClaimId == null || nextClaimId.isBlank()) {
            return INITIAL_CLAIM_ID;
        }
        return nextClaimId;
    }

    private void sleepAfterError() {
        try {
            Thread.sleep(ERROR_BACKOFF_MILLIS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + sequence.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
    }

    private void blockUntilQueueHasCapacity(Runnable task, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            throw new RejectedExecutionException("Redis Stream processing executor is shut down");
        }
        try {
            executor.getQueue().put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(
                    "Interrupted while waiting for Redis Stream processing capacity", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }
        consumers.values().forEach(consumer -> consumer.active.set(false));
        consumers.values().stream()
                .map(consumer -> consumer.future)
                .filter(future -> future != null)
                .forEach(future -> future.cancel(true));
        coordinatorExecutor.shutdownNow();
        processingExecutor.shutdown();
        try {
            if (!processingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                processingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            processingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class EventConsumer {
        private final Long eventId;
        private final String streamKey;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final AtomicBoolean drainRequested = new AtomicBoolean();
        private final AtomicInteger inFlight = new AtomicInteger();
        private final Set<String> inFlightRecordIds = ConcurrentHashMap.newKeySet();
        private volatile String nextClaimId = INITIAL_CLAIM_ID;
        private volatile Instant lastClaimAt = Instant.EPOCH;
        private volatile Instant drainedAt;
        private volatile Future<?> future;

        private EventConsumer(Long eventId, String streamKey) {
            this.eventId = eventId;
            this.streamKey = streamKey;
        }
    }
}
