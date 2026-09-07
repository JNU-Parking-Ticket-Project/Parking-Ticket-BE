package com.jnu.ticketbatch.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.AlreadyOpenStatusException;
import com.jnu.ticketdomain.domains.events.exception.RedisStockUnavailableException;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
import com.jnu.ticketinfrastructure.stream.RedisStreamConsumerManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuartzJobLauncherTest {

    @Mock private EventAdaptor eventAdaptor;
    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private RedisStreamConsumerManager streamConsumerManager;
    @Mock private JobExecutionContext context;
    @Mock private Event event;
    @Mock private Sector sector;

    private QuartzJobLauncher quartzJobLauncher;

    @BeforeEach
    void setUp() {
        quartzJobLauncher = new QuartzJobLauncher();
        ReflectionTestUtils.setField(quartzJobLauncher, "eventAdaptor", eventAdaptor);
        ReflectionTestUtils.setField(quartzJobLauncher, "sectorAdaptor", sectorAdaptor);
        ReflectionTestUtils.setField(quartzJobLauncher, "waitingQueueService", waitingQueueService);
        ReflectionTestUtils.setField(
                quartzJobLauncher, "streamConsumerManager", streamConsumerManager);
    }

    @Test
    @DisplayName("Redis 재고 초기화와 이벤트 OPEN이 끝난 뒤 Stream consumer를 시작한다")
    void executeInitializesRedisBeforeOpeningEventAndStartingConsumer() throws Exception {
        givenEventJob();
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(waitingQueueService.initializeEventStock(3L, List.of(sector))).thenReturn(true);

        quartzJobLauncher.execute(context);

        InOrder order =
                inOrder(sectorAdaptor, waitingQueueService, eventAdaptor, streamConsumerManager);
        order.verify(sectorAdaptor).findByEventId(3L);
        order.verify(waitingQueueService).initializeEventStock(3L, List.of(sector));
        order.verify(eventAdaptor).updateEventStatus(event, EventStatus.OPEN);
        order.verify(streamConsumerManager).start(3L);
    }

    @Test
    @DisplayName("Redis 연결 실패 시 이벤트를 OPEN하거나 consumer를 시작하지 않는다")
    void executeDoesNotOpenEventWhenRedisFails() {
        givenEventJob();
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        RedisConnectionFailureException failure =
                new RedisConnectionFailureException("Redis unavailable");
        when(waitingQueueService.initializeEventStock(3L, List.of(sector))).thenThrow(failure);

        assertThatThrownBy(() -> quartzJobLauncher.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasCause(failure);

        verify(eventAdaptor, never()).updateEventStatus(event, EventStatus.OPEN);
        verify(streamConsumerManager, never()).start(3L);
    }

    @Test
    @DisplayName("Redis 초기화가 false를 반환하면 이벤트를 OPEN하거나 consumer를 시작하지 않는다")
    void executeDoesNotOpenEventWhenRedisInitializationReturnsFalse() {
        givenEventJob();
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(waitingQueueService.initializeEventStock(3L, List.of(sector))).thenReturn(false);

        assertThatThrownBy(() -> quartzJobLauncher.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasCause(RedisStockUnavailableException.EXCEPTION);

        verify(eventAdaptor, never()).updateEventStatus(event, EventStatus.OPEN);
        verify(streamConsumerManager, never()).start(3L);
    }

    @Test
    @DisplayName("이미 OPEN된 이벤트의 스케줄 재실행은 Redis를 덮어쓰지 않고 거부한다")
    void executeRejectsRepeatedOpenRequest() {
        givenEventJob();
        doThrow(AlreadyOpenStatusException.EXCEPTION).when(event).validateReadyToOpen();

        assertThatThrownBy(() -> quartzJobLauncher.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasCause(AlreadyOpenStatusException.EXCEPTION);

        verify(sectorAdaptor, never()).findByEventId(3L);
        verify(waitingQueueService, never())
                .initializeEventStock(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyList());
        verify(eventAdaptor, never()).updateEventStatus(event, EventStatus.OPEN);
        verify(streamConsumerManager, never()).start(3L);
    }

    @Test
    @DisplayName("scale-down 환경에서는 Redis 없이 이벤트를 OPEN하지 않는다")
    void executeDoesNotOpenEventWhenRedisIsDisabled() {
        givenEventJob();
        ReflectionTestUtils.setField(quartzJobLauncher, "waitingQueueService", null);
        ReflectionTestUtils.setField(quartzJobLauncher, "streamConsumerManager", null);

        assertThatThrownBy(() -> quartzJobLauncher.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasCause(RedisStockUnavailableException.EXCEPTION);

        verify(sectorAdaptor, never()).findByEventId(3L);
        verify(eventAdaptor, never()).updateEventStatus(event, EventStatus.OPEN);
    }

    private void givenEventJob() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("eventId", 3L);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
        when(eventAdaptor.findById(3L)).thenReturn(event);
    }
}
