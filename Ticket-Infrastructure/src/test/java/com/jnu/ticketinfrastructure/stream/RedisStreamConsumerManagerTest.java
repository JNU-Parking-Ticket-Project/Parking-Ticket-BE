package com.jnu.ticketinfrastructure.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketinfrastructure.model.AutoClaimResult;
import com.jnu.ticketinfrastructure.model.ChatMessage;
import com.jnu.ticketinfrastructure.model.DeadLetterTransferResult;
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
    @DisplayName("역직렬화 실패도 delivery 실패로 기록해 poison message 상한을 적용한다")
    void recordsDeserializationFailureForDeadLetterPolicy() {
        WaitingQueueService waitingQueueService = mock(WaitingQueueService.class);
        RegistrationStreamMessageHandler handler = mock(RegistrationStreamMessageHandler.class);
        RawStreamMessage rawMessage = new RawStreamMessage("3-0", "not-json");
        IllegalStateException failure = new IllegalStateException("invalid payload");
        configureQueue(waitingQueueService);
        when(waitingQueueService.readNewMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class)))
                .thenReturn(List.of(rawMessage), List.of());
        when(waitingQueueService.deserialize(STREAM_KEY, rawMessage)).thenThrow(failure);
        when(waitingQueueService.recordProcessingFailure(
                        STREAM_KEY,
                        "쿠폰 발급 그룹",
                        "3-0",
                        "not-json",
                        3,
                        failure))
                .thenReturn(new DeadLetterTransferResult(1, false));
        consumerManager = manager(waitingQueueService, handler, 2, 2, 0L);

        consumerManager.start(EVENT_ID);

        verify(waitingQueueService, timeout(2000))
                .recordProcessingFailure(
                        STREAM_KEY,
                        "쿠폰 발급 그룹",
                        "3-0",
                        "not-json",
                        3,
                        failure);
        verify(handler, never()).handle(any());
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

        assertThat(consumerManager.requestDrain(EVENT_ID)).isTrue();

        assertThat(waitUntilStopped()).isTrue();
    }

    @Test
    @DisplayName("구독 없이 종료된 이벤트에 메시지가 남으면 consumer를 복원해 drain한다")
    void restoresMissingConsumerForClosedEventDrain() {
        WaitingQueueService waitingQueueService = mock(WaitingQueueService.class);
        RegistrationStreamMessageHandler handler = mock(RegistrationStreamMessageHandler.class);
        RawStreamMessage rawMessage = new RawStreamMessage("5-0", "payload");
        StreamQueueMessage streamQueueMessage = streamQueueMessage("5-0");
        configureQueue(waitingQueueService);
        when(waitingQueueService.hasEventStreamMessages(EVENT_ID)).thenReturn(true);
        when(waitingQueueService.readNewMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class)))
                .thenReturn(List.of(rawMessage), List.of());
        when(waitingQueueService.deserialize(STREAM_KEY, rawMessage))
                .thenReturn(streamQueueMessage);
        when(waitingQueueService.getConsumerState(STREAM_KEY, "쿠폰 발급 그룹", 0))
                .thenReturn(new StreamConsumerState(0L, 0L, 0));
        consumerManager = manager(waitingQueueService, handler, 2, 2, 0L);

        assertThat(consumerManager.requestDrain(EVENT_ID)).isTrue();

        verify(handler, timeout(2000)).handle(streamQueueMessage);
        assertThat(consumerManager.awaitDrainCompletion(EVENT_ID, Duration.ofSeconds(2))).isTrue();
    }

    @Test
    @DisplayName("종료된 이벤트 Stream에 메시지가 없으면 불필요한 consumer를 만들지 않는다")
    void skipsDrainConsumerWhenClosedEventStreamIsEmpty() {
        WaitingQueueService waitingQueueService = mock(WaitingQueueService.class);
        RegistrationStreamMessageHandler handler = mock(RegistrationStreamMessageHandler.class);
        when(waitingQueueService.hasEventStreamMessages(EVENT_ID)).thenReturn(false);
        consumerManager = manager(waitingQueueService, handler, 2, 2, 0L);

        assertThat(consumerManager.requestDrain(EVENT_ID)).isTrue();

        assertThat(consumerManager.isRunning(EVENT_ID)).isFalse();
        verify(waitingQueueService, never()).readNewMessages(any(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("최종 drain은 신규 dispatch를 중단하고 in-flight DB 처리가 끝날 때까지 대기한다")
    void finalDrainWaitsForInFlightProcessing() throws Exception {
        WaitingQueueService waitingQueueService = mock(WaitingQueueService.class);
        RawStreamMessage rawMessage = new RawStreamMessage("4-0", "payload");
        CountDownLatch processingStarted = new CountDownLatch(1);
        CountDownLatch releaseProcessing = new CountDownLatch(1);
        CountDownLatch processingFinished = new CountDownLatch(1);
        RegistrationStreamMessageHandler handler =
                message -> {
                    processingStarted.countDown();
                    try {
                        releaseProcessing.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        processingFinished.countDown();
                    }
                };
        configureQueue(waitingQueueService);
        when(waitingQueueService.readNewMessages(
                        eq(STREAM_KEY), any(), any(), anyLong(), any(Duration.class)))
                .thenReturn(List.of(rawMessage), List.of());
        when(waitingQueueService.deserialize(STREAM_KEY, rawMessage))
                .thenReturn(streamQueueMessage("4-0"));
        consumerManager = manager(waitingQueueService, handler, 2, 2, 0L);
        consumerManager.start(EVENT_ID);

        assertThat(processingStarted.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(consumerManager.pauseForFinalDrain(EVENT_ID)).isFalse();

        releaseProcessing.countDown();
        assertThat(processingFinished.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(waitUntilFinalDrainIsReady()).isTrue();
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
                3,
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

    private boolean waitUntilFinalDrainIsReady() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (consumerManager.pauseForFinalDrain(EVENT_ID)) {
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
