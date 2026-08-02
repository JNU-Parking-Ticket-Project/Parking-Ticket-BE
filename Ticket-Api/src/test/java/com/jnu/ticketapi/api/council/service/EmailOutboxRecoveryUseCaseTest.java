package com.jnu.ticketapi.api.council.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.council.model.response.FailedEmailOutboxesResponse;
import com.jnu.ticketapi.api.council.model.response.RequeueEmailOutboxResponse;
import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.email.exception.FailedEmailOutboxNotFoundException;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class EmailOutboxRecoveryUseCaseTest {

    @Mock private EventAdaptor eventAdaptor;
    @Mock private EmailOutboxAdaptor emailOutboxAdaptor;
    @Mock private Event event;
    @Mock private EmailOutbox outbox;

    private EmailOutboxRecoveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EmailOutboxRecoveryUseCase(eventAdaptor, emailOutboxAdaptor);
    }

    @Test
    @DisplayName("최종 실패 Outbox를 이벤트 단위로 조회하고 페이지 크기를 100건으로 제한한다")
    void findsFailedOutboxesWithBoundedPageSize() {
        PageRequest requestedPage = PageRequest.of(2, 500);
        PageRequest boundedPage = PageRequest.of(2, 100);
        LocalDateTime failedAt = LocalDateTime.of(2026, 7, 29, 12, 0);
        givenFailedOutbox(failedAt);
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(emailOutboxAdaptor.findFailedByEventId(10L, boundedPage))
                .thenReturn(new PageImpl<>(List.of(outbox), boundedPage, 201));

        FailedEmailOutboxesResponse response = useCase.findFailed(10L, requestedPage);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.totalElements()).isEqualTo(201);
        assertThat(response.outboxes()).hasSize(1);
        assertThat(response.outboxes().get(0).lastError()).isEqualTo("SES unavailable");
        assertThat(response.outboxes().get(0).failedAt()).isEqualTo(failedAt);
        verify(eventAdaptor).findById(10L);
        verify(emailOutboxAdaptor).findFailedByEventId(10L, boundedPage);
    }

    @Test
    @DisplayName("최종 실패 Outbox 재처리는 기존 worker가 읽는 PENDING 상태로 복원한다")
    void requeuesFailedOutbox() {
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(emailOutboxAdaptor.requeueFailed(10L, 1L)).thenReturn(true);

        RequeueEmailOutboxResponse response = useCase.requeue(10L, 1L);

        assertThat(response.outboxId()).isEqualTo(1L);
        assertThat(response.eventId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("최종 실패가 아니거나 이미 재처리된 Outbox는 다시 재처리할 수 없다")
    void rejectsOutboxThatIsNotFailed() {
        when(eventAdaptor.findById(10L)).thenReturn(event);
        when(emailOutboxAdaptor.requeueFailed(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.requeue(10L, 1L))
                .isSameAs(FailedEmailOutboxNotFoundException.EXCEPTION);
    }

    private void givenFailedOutbox(LocalDateTime failedAt) {
        when(outbox.getId()).thenReturn(1L);
        when(outbox.getEventId()).thenReturn(10L);
        when(outbox.getRegistrationId()).thenReturn(100L);
        when(outbox.getEmail()).thenReturn("student@jnu.ac.kr");
        when(outbox.getName()).thenReturn("학생");
        when(outbox.getResultStatus()).thenReturn(UserStatus.SUCCESS);
        when(outbox.getSequence()).thenReturn(-2);
        when(outbox.getRetryCount()).thenReturn(10);
        when(outbox.getFailedAt()).thenReturn(failedAt);
        when(outbox.getLastError()).thenReturn("SES unavailable");
    }
}
