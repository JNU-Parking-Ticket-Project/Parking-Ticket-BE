package com.jnu.ticketapi.api.registration.handler;

import static com.jnu.ticketcommon.consts.TicketStatic.MAX_EMAIL_SEND_RETRY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.service.MailService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailOutboxWorkerTest {

    @Mock private EmailOutboxAdaptor emailOutboxAdaptor;
    @Mock private MailService mailService;
    @Mock private EmailOutbox outbox;

    private EmailOutboxWorker emailOutboxWorker;

    @BeforeEach
    void setUp() {
        emailOutboxWorker = new EmailOutboxWorker(emailOutboxAdaptor, mailService);
    }

    @Test
    @DisplayName("claim에 성공한 outbox 메일 발송이 성공하면 sent_at을 기록한다")
    void sendPendingMarksSentWhenMailSucceeded() {
        givenPendingOutbox();
        when(emailOutboxAdaptor.claim(eq(1L), any(LocalDateTime.class), eq(MAX_EMAIL_SEND_RETRY)))
                .thenReturn(true);
        when(mailService.sendRegistrationResultMail("student@jnu.ac.kr", "학생", "합격", -2))
                .thenReturn(true);

        emailOutboxWorker.sendPendingRegistrationResultMail();

        verify(emailOutboxAdaptor).markSent(1L);
        verify(emailOutboxAdaptor, never()).markFailed(1L);
    }

    @Test
    @DisplayName("claim에 실패한 outbox는 메일을 보내지 않는다")
    void sendPendingSkipsWhenClaimFailed() {
        givenPendingOutboxWithoutMailPayload();
        when(emailOutboxAdaptor.claim(eq(1L), any(LocalDateTime.class), eq(MAX_EMAIL_SEND_RETRY)))
                .thenReturn(false);

        emailOutboxWorker.sendPendingRegistrationResultMail();

        verify(mailService, never()).sendRegistrationResultMail(any(), any(), any(), any());
        verify(emailOutboxAdaptor, never()).markSent(1L);
    }

    @Test
    @DisplayName("메일 발송이 실패하면 실패 시각과 재시도 횟수를 기록한다")
    void sendPendingMarksOutboxFailedWhenMailFailed() {
        givenPendingOutbox();
        when(emailOutboxAdaptor.claim(eq(1L), any(LocalDateTime.class), eq(MAX_EMAIL_SEND_RETRY)))
                .thenReturn(true);
        when(mailService.sendRegistrationResultMail("student@jnu.ac.kr", "학생", "합격", -2))
                .thenReturn(false);

        emailOutboxWorker.sendPendingRegistrationResultMail();

        verify(emailOutboxAdaptor).markFailed(1L);
        verify(emailOutboxAdaptor, never()).markSent(1L);
    }

    @Test
    @DisplayName("단일 worker는 한 번에 최대 14건의 outbox를 조회한다")
    void findsAtMostFourteenPendingOutboxesPerRun() {
        when(emailOutboxAdaptor.findPending(
                        eq(14), any(LocalDateTime.class), eq(MAX_EMAIL_SEND_RETRY)))
                .thenReturn(List.of());

        emailOutboxWorker.sendPendingRegistrationResultMail();

        verify(emailOutboxAdaptor)
                .findPending(eq(14), any(LocalDateTime.class), eq(MAX_EMAIL_SEND_RETRY));
    }

    private void givenPendingOutbox() {
        givenPendingOutboxWithoutMailPayload();
        when(outbox.getEmail()).thenReturn("student@jnu.ac.kr");
        when(outbox.getName()).thenReturn("학생");
        when(outbox.getResultStatus()).thenReturn(UserStatus.SUCCESS);
        when(outbox.getSequence()).thenReturn(-2);
    }

    private void givenPendingOutboxWithoutMailPayload() {
        when(emailOutboxAdaptor.findPending(
                        eq(14), any(LocalDateTime.class), eq(MAX_EMAIL_SEND_RETRY)))
                .thenReturn(List.of(outbox));
        when(outbox.getId()).thenReturn(1L);
    }
}
