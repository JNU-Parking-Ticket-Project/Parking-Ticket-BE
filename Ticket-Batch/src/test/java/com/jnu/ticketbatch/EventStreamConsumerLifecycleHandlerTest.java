package com.jnu.ticketbatch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.event.EventStatusChangeEvent;
import com.jnu.ticketdomain.domains.events.exception.NotFoundEventException;
import com.jnu.ticketinfrastructure.stream.RedisStreamConsumerManager;
import java.time.Duration;
import java.util.List;
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
    @Mock private Event secondEvent;

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
        when(eventAdaptor.findClosedEvents()).thenReturn(List.of());

        lifecycleHandler.restoreOpenEventSubscription();

        verify(streamConsumerManager).start(3L);
    }

    @Test
    @DisplayName("애플리케이션 재시작 시 CLOSED 이벤트의 남은 Stream을 끝까지 drain한다")
    void restoresClosedEventDrainOnStartup() {
        when(eventAdaptor.findOpenEvent()).thenThrow(NotFoundEventException.EXCEPTION);
        when(eventAdaptor.findClosedEvents()).thenReturn(List.of(event, secondEvent));
        when(event.getId()).thenReturn(3L);
        when(secondEvent.getId()).thenReturn(4L);
        when(streamConsumerManager.requestDrain(3L)).thenReturn(true);
        when(streamConsumerManager.requestDrain(4L)).thenReturn(true);
        when(streamConsumerManager.awaitDrainCompletion(3L, Duration.ofMinutes(5)))
                .thenReturn(true);
        when(streamConsumerManager.awaitDrainCompletion(4L, Duration.ofMinutes(5)))
                .thenReturn(true);

        lifecycleHandler.restoreOpenEventSubscription();

        verify(streamConsumerManager).requestDrain(3L);
        verify(streamConsumerManager).awaitDrainCompletion(3L, Duration.ofMinutes(5));
        verify(streamConsumerManager).requestDrain(4L);
        verify(streamConsumerManager).awaitDrainCompletion(4L, Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("CLOSED 이벤트 Stream을 제한 시간 안에 복원하지 못하면 기동을 실패시킨다")
    void failsStartupWhenClosedEventDrainCannotBeRestored() {
        when(eventAdaptor.findOpenEvent()).thenThrow(NotFoundEventException.EXCEPTION);
        when(eventAdaptor.findClosedEvents()).thenReturn(List.of(event));
        when(event.getId()).thenReturn(3L);
        when(streamConsumerManager.requestDrain(3L)).thenReturn(true);
        when(streamConsumerManager.awaitDrainCompletion(3L, Duration.ofMinutes(5)))
                .thenReturn(false);

        assertThatThrownBy(lifecycleHandler::restoreOpenEventSubscription)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("eventId=3");
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
