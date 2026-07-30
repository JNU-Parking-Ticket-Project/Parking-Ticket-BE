package com.jnu.ticketbatch.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketinfrastructure.stream.RedisStreamConsumerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuartzJobLauncherTest {

    @Mock private EventAdaptor eventAdaptor;
    @Mock private RedisStreamConsumerManager streamConsumerManager;
    @Mock private JobExecutionContext context;
    @Mock private Event event;

    private QuartzJobLauncher quartzJobLauncher;

    @BeforeEach
    void setUp() {
        quartzJobLauncher = new QuartzJobLauncher();
        ReflectionTestUtils.setField(quartzJobLauncher, "eventAdaptor", eventAdaptor);
        ReflectionTestUtils.setField(
                quartzJobLauncher, "streamConsumerManager", streamConsumerManager);
    }

    @Test
    @DisplayName("OPEN Quartz Job은 이벤트를 연 뒤 Stream consumer를 시작한다")
    void startsStreamConsumerAfterOpeningEvent() throws Exception {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("eventId", 3L);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
        when(eventAdaptor.findById(3L)).thenReturn(event);

        quartzJobLauncher.execute(context);

        verify(eventAdaptor).updateEventStatus(event, EventStatus.OPEN);
        verify(streamConsumerManager).start(3L);
    }
}
