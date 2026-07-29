package com.jnu.ticketinfrastructure.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketinfrastructure.model.AutoClaimResult;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.RawStreamMessage;
import com.jnu.ticketinfrastructure.model.StreamConsumerState;
import com.jnu.ticketinfrastructure.model.StreamQueueMessage;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class RedisStreamConsumerManagerTest {

    private static final Long EVENT_ID = 3L;
    private static final String STREAM_KEY = "쿠폰 발급 스트림:{3}";

    private RedisStreamConsumerManager consumerManager;

    @AfterEach
    void tearDown() {
        if (consumerManager != null) {
            consumerManager.shutdown();
        }
    }

    @Test
    @DisplayName("blocking consumer가 ApplicationEvent 없이 Stream handler를 직접 호출한다")
    void consumesNewMessageDirectly() {
        WaitingQueueService waitingQueueService = mock(WaitingQueueService.class);
        RegistrationStreamMessageHandler handler = mock(RegistrationStreamMessageHandler.class);
        RawStreamMessage rawMessage = new RawStreamMessage("1-0", "payload");
        StreamQueueMessage streamQueueMessage = streamQueueMessage("1-0");
        configureQueue(waitingQueueService);
        when(waitingQueueService.readNewMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class)))
                .thenReturn(List.of(rawMessage), List.of());
        when(waitingQueueService.deserialize(STREAM_KEY, rawMessage))
                .thenReturn(streamQueueMessage);
        consumerManager = manager(waitingQueueService, handler, 2, 2, 0L);

        consumerManager.start(EVENT_ID);

        verify(handler, timeout(2000)).handle(streamQueueMessage);
    }

    @Test
    @DisplayName("XAUTOCLAIM으로 회수한 stale pending 메시지도 같은 handler로 처리한다")
    void consumesAutoClaimedPendingMessage() {
        WaitingQueueService waitingQueueService = mock(WaitingQueueService.class);
        RegistrationStreamMessageHandler handler = mock(RegistrationStreamMessageHandler.class);
        RawStreamMessage rawMessage = new RawStreamMessage("2-0", "payload");
        StreamQueueMessage streamQueueMessage = streamQueueMessage("2-0");
        when(waitingQueueService.eventStreamKey(EVENT_ID)).thenReturn(STREAM_KEY);
        when(waitingQueueService.autoClaimMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class), any()))
                .thenReturn(
                        new AutoClaimResult("0-0", List.of(rawMessage)), AutoClaimResult.empty());
        when(waitingQueueService.readNewMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class)))
                .thenReturn(List.of());
        when(waitingQueueService.deserialize(STREAM_KEY, rawMessage))
                .thenReturn(streamQueueMessage);
        consumerManager = manager(waitingQueueService, handler, 2, 2, 0L);

        consumerManager.start(EVENT_ID);

        verify(handler, timeout(2000)).handle(streamQueueMessage);
    }

    @Test
    @DisplayName("동시 DB 처리는 Hikari 여유 connection 수를 넘지 않는다")
    void limitsConcurrentProcessingToAvailableDbConnections() throws Exception {
        WaitingQueueService waitingQueueService = mock(WaitingQueueService.class);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        CountDownLatch twoWorkersStarted = new CountDownLatch(2);
        CountDownLatch allMessagesProcessed = new CountDownLatch(6);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        RegistrationStreamMessageHandler handler =
                message -> {
                    int current = active.incrementAndGet();
                    maxActive.accumulateAndGet(current, Math::max);
                    twoWorkersStarted.countDown();
                    try {
                        releaseWorkers.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        active.decrementAndGet();
                        allMessagesProcessed.countDown();
                    }
                };
        List<RawStreamMessage> messages =
                java.util.stream.IntStream.rangeClosed(1, 6)
                        .mapToObj(index -> new RawStreamMessage(index + "-0", "payload"))
                        .toList();
        configureQueue(waitingQueueService);
        when(waitingQueueService.readNewMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class)))
                .thenReturn(messages, List.of());
        when(waitingQueueService.deserialize(eq(STREAM_KEY), any(RawStreamMessage.class)))
                .thenAnswer(
                        invocation ->
                                streamQueueMessage(
                                        invocation
                                                .getArgument(1, RawStreamMessage.class)
                                                .getRecordId()));
        consumerManager = manager(waitingQueueService, handler, 3, 10, 0L);

        consumerManager.start(EVENT_ID);

        assertThat(twoWorkersStarted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(maxActive).hasValue(2);
        releaseWorkers.countDown();
        assertThat(allMessagesProcessed.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("종료 요청 후 lag, pending, in-flight가 모두 0이면 구독을 해제한다")
    void stopsAfterStreamIsDrained() {
        WaitingQueueService waitingQueueService = mock(WaitingQueueService.class);
        RegistrationStreamMessageHandler handler = mock(RegistrationStreamMessageHandler.class);
        configureQueue(waitingQueueService);
        when(waitingQueueService.readNewMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class)))
                .thenReturn(List.of());
        when(waitingQueueService.getConsumerState(STREAM_KEY, "쿠폰 발급 그룹", 0))
                .thenReturn(new StreamConsumerState(0L, 0L, 0));
        consumerManager = manager(waitingQueueService, handler, 2, 2, 0L);
        consumerManager.start(EVENT_ID);

        consumerManager.requestDrain(EVENT_ID);

        assertThat(waitUntilStopped()).isTrue();
    }

    private void configureQueue(WaitingQueueService waitingQueueService) {
        when(waitingQueueService.eventStreamKey(EVENT_ID)).thenReturn(STREAM_KEY);
        when(waitingQueueService.autoClaimMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class), any()))
                .thenReturn(AutoClaimResult.empty());
    }

    private RedisStreamConsumerManager manager(
            WaitingQueueService waitingQueueService,
            RegistrationStreamMessageHandler handler,
            int dbPoolSize,
            int configuredConcurrency,
            long drainQuietPeriodMillis) {
        @SuppressWarnings("unchecked")
        ObjectProvider<RegistrationStreamMessageHandler> handlerProvider =
                mock(ObjectProvider.class);
        when(handlerProvider.getIfAvailable()).thenReturn(handler);
        return new RedisStreamConsumerManager(
                waitingQueueService,
                handlerProvider,
                dbPoolSize,
                1,
                configuredConcurrency,
                1,
                1,
                10,
                10L,
                10L,
                1L,
                drainQuietPeriodMillis);
    }

    private StreamQueueMessage streamQueueMessage(String recordId) {
        return new StreamQueueMessage(
                STREAM_KEY, recordId, new ChatMessage("{}", 1L, 2L, EVENT_ID));
    }

    private boolean waitUntilStopped() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (!consumerManager.isRunning(EVENT_ID)) {
                return true;
            }
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
