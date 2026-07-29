package com.jnu.ticketapi.api.council.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.domains.council.adaptor.CouncilAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.event.SendEmailEvent;
import com.jnu.ticketdomain.domains.events.exception.StillOpenEventException;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CouncilUseCaseTest {

    @Mock private CouncilAdaptor councilAdaptor;
    @Mock private UserAdaptor userAdaptor;
    @Mock private EventAdaptor eventAdaptor;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Event event;

    private CouncilUseCase councilUseCase;

    @BeforeEach
    void setUp() {
        councilUseCase = new CouncilUseCase(councilAdaptor, userAdaptor, eventAdaptor);
        Events.setPublisher(eventPublisher);
    }

    @AfterEach
    void tearDown() {
        Events.reset();
    }

    @Test
    @DisplayName("종료된 이벤트의 수동 메일 요청은 결과를 재집계하지 않고 메일 이벤트만 발행한다")
    void sendsEmailWithoutReassigningResults() {
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(event.getEventStatus()).thenReturn(EventStatus.CLOSED);

        var response = councilUseCase.sendEmail(10L);

        assertThat(response.message()).isEqualTo(ResponseMessage.SUCCESS_SEND_EMAIL_MANUALLY);
        ArgumentCaptor<SendEmailEvent> eventCaptor = ArgumentCaptor.forClass(SendEmailEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("종료되지 않은 이벤트의 수동 메일 요청은 실패 응답으로 숨기지 않고 예외를 전달한다")
    void rejectsOpenEvent() {
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(event.getEventStatus()).thenReturn(EventStatus.OPEN);

        assertThatThrownBy(() -> councilUseCase.sendEmail(10L))
                .isSameAs(StillOpenEventException.EXCEPTION);
        verifyNoInteractions(eventPublisher);
    }
}
