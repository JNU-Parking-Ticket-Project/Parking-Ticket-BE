package com.jnu.ticketapi.api.council.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.events.event.SendEmailEvent;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class EmailSendEventHandlerTest {

    @Mock private RegistrationAdaptor registrationAdaptor;
    @Mock private EmailOutboxAdaptor emailOutboxAdaptor;
    @Mock private Registration firstRegistration;
    @Mock private Registration secondRegistration;
    @Mock private Registration thirdRegistration;

    private EmailSendEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new EmailSendEventHandler(registrationAdaptor, emailOutboxAdaptor);
    }

    @Test
    @DisplayName("수동 메일 요청은 저장 완료 신청을 마지막 페이지까지 Outbox로 등록한다")
    void createsOutboxesForEveryRegistrationPage() {
        when(registrationAdaptor.findByIsDeletedFalseAndIsSavedTrueByPage(10L, 0))
                .thenReturn(
                        new PageImpl<>(
                                List.of(firstRegistration, secondRegistration),
                                PageRequest.of(0, 2),
                                3));
        when(registrationAdaptor.findByIsDeletedFalseAndIsSavedTrueByPage(10L, 1))
                .thenReturn(
                        new PageImpl<>(List.of(thirdRegistration), PageRequest.of(1, 2), 3));

        handler.handle(new SendEmailEvent(10L));

        InOrder order = inOrder(registrationAdaptor, emailOutboxAdaptor);
        order.verify(registrationAdaptor).findByIsDeletedFalseAndIsSavedTrueByPage(10L, 0);
        order.verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(firstRegistration);
        order.verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(secondRegistration);
        order.verify(registrationAdaptor).findByIsDeletedFalseAndIsSavedTrueByPage(10L, 1);
        order.verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(thirdRegistration);
    }

    @Test
    @DisplayName("Outbox 생성 실패는 수동 메일 요청 성공으로 숨기지 않는다")
    void propagatesOutboxCreationFailure() {
        when(registrationAdaptor.findByIsDeletedFalseAndIsSavedTrueByPage(10L, 0))
                .thenReturn(new PageImpl<>(List.of(firstRegistration, secondRegistration)));
        doThrow(new IllegalStateException("확정 결과 누락"))
                .when(emailOutboxAdaptor)
                .saveRegistrationResultIfAbsent(firstRegistration);

        assertThatThrownBy(() -> handler.handle(new SendEmailEvent(10L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("확정 결과 누락");
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(firstRegistration);
        verify(emailOutboxAdaptor, never()).saveRegistrationResultIfAbsent(secondRegistration);
    }
}
