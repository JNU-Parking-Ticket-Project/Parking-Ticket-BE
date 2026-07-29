package com.jnu.ticketbatch.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketinfrastructure.service.WaitingQueueService;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuartzJobLauncherTest {

    @Mock private EventAdaptor eventAdaptor;
    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private WaitingQueueService waitingQueueService;
    @Mock private JobExecutionContext context;
    @Mock private Event event;
    @Mock private Sector sector;

    private QuartzJobLauncher quartzJobLauncher;

    @BeforeEach
    void setUp() {
        quartzJobLauncher = new QuartzJobLauncher();
        ReflectionTestUtils.setField(quartzJobLauncher, "eventAdaptor", eventAdaptor);
        ReflectionTestUtils.setField(quartzJobLauncher, "sectorAdaptor", sectorAdaptor);
        ReflectionTestUtils.setField(
                quartzJobLauncher, "waitingQueueService", waitingQueueService);
    }

    @Test
    @DisplayName("구간별 Redis 재고 초기화가 완료된 뒤 이벤트를 OPEN으로 전환한다")
    void executeInitializesRedisBeforeOpeningEvent() throws Exception {
        givenEventJob();
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(waitingQueueService.initializeEventStock(3L, List.of(sector))).thenReturn(true);

        quartzJobLauncher.execute(context);

        InOrder order = inOrder(sectorAdaptor, waitingQueueService, eventAdaptor);
        order.verify(sectorAdaptor).findByEventId(3L);
        order.verify(waitingQueueService).initializeEventStock(3L, List.of(sector));
        order.verify(eventAdaptor).updateEventStatus(event, EventStatus.OPEN);
    }

    @Test
    @DisplayName("Redis 초기화에 실패하면 이벤트를 OPEN으로 변경하지 않는다")
    void executeKeepsEventReadyWhenRedisInitializationFails() {
        givenEventJob();
        when(sectorAdaptor.findByEventId(3L)).thenReturn(List.of(sector));
        when(waitingQueueService.initializeEventStock(3L, List.of(sector)))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        assertThatThrownBy(() -> quartzJobLauncher.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("initialize and open");
        verify(eventAdaptor, never()).updateEventStatus(event, EventStatus.OPEN);
    }

    @Test
    @DisplayName("Redis가 비활성화된 환경에서는 기존처럼 DB 이벤트만 OPEN으로 전환한다")
    void executeOpensEventWhenRedisIsDisabled() {
        givenEventJob();
        ReflectionTestUtils.setField(quartzJobLauncher, "waitingQueueService", null);

        assertThatCode(() -> quartzJobLauncher.execute(context)).doesNotThrowAnyException();

        verify(sectorAdaptor, never()).findByEventId(3L);
        verify(eventAdaptor).updateEventStatus(event, EventStatus.OPEN);
    }

    private void givenEventJob() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("eventId", 3L);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
        when(eventAdaptor.findById(3L)).thenReturn(event);
    }
}
