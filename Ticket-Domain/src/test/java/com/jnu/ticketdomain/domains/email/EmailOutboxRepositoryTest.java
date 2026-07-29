package com.jnu.ticketdomain.domains.email;

import static com.jnu.ticketcommon.consts.TicketStatic.MAX_EMAIL_SEND_RETRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.email.repository.EmailOutboxRepository;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class EmailOutboxRepositoryTest {

    @Autowired private EmailOutboxRepository emailOutboxRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("재시도 대기 중이거나 최대 재시도에 도달한 outbox는 pending 조회와 claim에서 제외한다")
    void findPendingExcludesCoolingDownAndExhaustedOutboxes() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 28, 12, 0);
        LocalDateTime staleBefore = now.minusMinutes(10);
        EmailOutbox ready = emailOutboxRepository.saveAndFlush(outbox(1L));
        EmailOutbox coolingDown = emailOutboxRepository.saveAndFlush(outbox(2L));
        EmailOutbox exhausted = emailOutboxRepository.saveAndFlush(outbox(3L));

        markFailed(coolingDown.getId(), now.minusMinutes(1), "temporary failure");
        for (int retry = 0; retry < MAX_EMAIL_SEND_RETRY; retry++) {
            markFailed(exhausted.getId(), now.minusMinutes(20), "permanent failure");
        }
        entityManager.clear();

        List<EmailOutbox> pending =
                emailOutboxRepository.findPending(
                        staleBefore, MAX_EMAIL_SEND_RETRY, PageRequest.of(0, 20));

        assertThat(pending)
                .extracting(EmailOutbox::getRegistrationId)
                .containsExactly(ready.getRegistrationId());
        assertThat(
                        emailOutboxRepository.claim(
                                exhausted.getId(), now, staleBefore, MAX_EMAIL_SEND_RETRY))
                .isZero();
    }

    @Test
    @DisplayName("마지막 재시도 실패 시 failed_at을 기록하고 processing 상태를 해제한다")
    void markFailureTransitionsToTerminalStateAtRetryLimit() {
        EmailOutbox outbox = emailOutboxRepository.saveAndFlush(outbox(4L));
        LocalDateTime firstFailureAt = LocalDateTime.of(2026, 7, 28, 12, 0);
        LocalDateTime terminalFailureAt = firstFailureAt.plusMinutes(20);

        markFailed(outbox.getId(), firstFailureAt, "first failure", 2);

        EmailOutbox retrying = emailOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(retrying.getRetryCount()).isEqualTo(1);
        assertThat(retrying.getProcessingAt()).isEqualTo(firstFailureAt);
        assertThat(retrying.getFailedAt()).isNull();
        assertThat(retrying.getLastError()).isEqualTo("first failure");

        markFailed(outbox.getId(), terminalFailureAt, "terminal failure", 2);

        EmailOutbox failed = emailOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(failed.getRetryCount()).isEqualTo(2);
        assertThat(failed.getProcessingAt()).isNull();
        assertThat(failed.getFailedAt()).isEqualTo(terminalFailureAt);
        assertThat(failed.getLastError()).isEqualTo("terminal failure");
        assertThat(emailOutboxRepository.markSent(outbox.getId(), terminalFailureAt.plusSeconds(1)))
                .isZero();
        assertThat(emailOutboxRepository.findById(outbox.getId()).orElseThrow().getSentAt())
                .isNull();
        assertThat(emailOutboxRepository.findFailedByEventId(10L, PageRequest.of(0, 20)))
                .extracting(EmailOutbox::getId)
                .containsExactly(outbox.getId());
    }

    @Test
    @DisplayName("최종 실패 outbox 재활성화는 한 번만 성공하고 발송 상태를 초기화한다")
    void requeueFailedOutboxIsAtomicAndIdempotent() {
        EmailOutbox outbox = emailOutboxRepository.saveAndFlush(outbox(5L));
        LocalDateTime failedAt = LocalDateTime.of(2026, 7, 28, 12, 0);
        markFailed(outbox.getId(), failedAt, "terminal failure", 1);

        int firstRequeue = emailOutboxRepository.requeueFailed(10L, outbox.getId());
        int repeatedRequeue = emailOutboxRepository.requeueFailed(10L, outbox.getId());

        assertThat(firstRequeue).isOne();
        assertThat(repeatedRequeue).isZero();
        EmailOutbox requeued = emailOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(requeued.getRetryCount()).isZero();
        assertThat(requeued.getProcessingAt()).isNull();
        assertThat(requeued.getFailedAt()).isNull();
        assertThat(requeued.getLastError()).isNull();
    }

    private void markFailed(Long id, LocalDateTime failedAt, String error) {
        markFailed(id, failedAt, error, MAX_EMAIL_SEND_RETRY);
    }

    private void markFailed(Long id, LocalDateTime failedAt, String error, int maxRetryCount) {
        int terminalRetryCount = maxRetryCount - 1;
        int updated =
                emailOutboxRepository.markTerminalFailure(
                        id, failedAt, error, terminalRetryCount, maxRetryCount);
        if (updated == 0) {
            emailOutboxRepository.markRetryFailure(id, failedAt, error, terminalRetryCount);
        }
    }

    private EmailOutbox outbox(Long registrationId) {
        Registration registration =
                Registration.builder()
                        .email("student" + registrationId + "@jnu.ac.kr")
                        .name("학생" + registrationId)
                        .studentNum("2024" + registrationId)
                        .carNum("12가" + registrationId)
                        .isLight(false)
                        .phoneNum("010-0000-0000")
                        .createdAt(LocalDateTime.of(2026, 7, 28, 10, 0))
                        .isSaved(true)
                        .eventId(10L)
                        .build();
        registration.setId(registrationId);
        registration.finalSave(1, UserStatus.SUCCESS, -2);
        return EmailOutbox.from(registration);
    }
}
