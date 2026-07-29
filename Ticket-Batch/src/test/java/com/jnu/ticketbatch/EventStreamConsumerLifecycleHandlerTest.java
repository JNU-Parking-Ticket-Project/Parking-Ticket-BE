package com.jnu.ticketbatch;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.event.EventStatusChangeEvent;
import com.jnu.ticketinfrastructure.stream.RedisStreamConsumerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventStreamConsumerLifecycleHandlerTest {

    @Mock private EventAdaptor eventAdaptor;
    @Mock private RedisStreamConsumerManager streamConsumerManager;
    @Mock private Event event;

    private EventStreamConsumerLifecycleHandler lifecycleHandler;

    @BeforeEach
    void setUp() {
        lifecycleHandler =
                new EventStreamConsumerLifecycleHandler(eventAdaptor, streamConsumerManager);
    }

    @Test
    @DisplayName("애플리케이션 재시작 시 OPEN 이벤트의 Stream 구독을 복원한다")
    void restoresOpenEventSubscriptionOnStartup() {
        when(eventAdaptor.findOpenEvent()).thenReturn(event);
        when(event.getId()).thenReturn(3L);

        lifecycleHandler.restoreOpenEventSubscription();

        verify(streamConsumerManager).start(3L);
    }

    @Test
    @DisplayName("이벤트가 OPEN으로 커밋되면 Stream 구독을 시작한다")
    void startsSubscriptionAfterEventOpen() {
        EventStatusChangeEvent statusChangeEvent = statusChangeEvent();
        when(eventAdaptor.findById(3L)).thenReturn(event);
        when(event.getId()).thenReturn(3L);
        when(event.getEventStatus()).thenReturn(EventStatus.OPEN);

        lifecycleHandler.handle(statusChangeEvent);

        verify(streamConsumerManager).start(3L);
    }

    @Test
    @DisplayName("이벤트가 CLOSED로 커밋되면 Stream drain을 요청한다")
    void requestsDrainAfterEventClose() {
        EventStatusChangeEvent statusChangeEvent = statusChangeEvent();
        when(eventAdaptor.findById(3L)).thenReturn(event);
        when(event.getId()).thenReturn(3L);
        when(event.getEventStatus()).thenReturn(EventStatus.CLOSED);

        lifecycleHandler.handle(statusChangeEvent);

        verify(streamConsumerManager).requestDrain(3L);
    }

    private EventStatusChangeEvent statusChangeEvent() {
        return EventStatusChangeEvent.builder().eventId(3L).eventCode("event").build();
    }
}
