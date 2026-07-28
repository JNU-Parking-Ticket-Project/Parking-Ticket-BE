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

        emailOutboxRepository.markFailed(coolingDown.getId(), now.minusMinutes(1));
        for (int retry = 0; retry < MAX_EMAIL_SEND_RETRY; retry++) {
            emailOutboxRepository.markFailed(exhausted.getId(), now.minusMinutes(20));
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
