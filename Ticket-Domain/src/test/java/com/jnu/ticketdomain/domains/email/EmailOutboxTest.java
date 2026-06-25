package com.jnu.ticketdomain.domains.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailOutboxTest {

    @Test
    @DisplayName("Registration에 확정된 결과가 있으면 해당 결과로 outbox를 생성한다")
    void fromUsesRegistrationDecision() {
        Registration registration = registration();
        registration.finalSave(3, UserStatus.PREPARE, 1);

        EmailOutbox outbox = EmailOutbox.from(registration);

        assertThat(outbox.getEventId()).isEqualTo(10L);
        assertThat(outbox.getRegistrationId()).isEqualTo(10L);
        assertThat(outbox.getEmail()).isEqualTo("student@jnu.ac.kr");
        assertThat(outbox.getName()).isEqualTo("학생");
        assertThat(outbox.getResultStatus()).isEqualTo(UserStatus.PREPARE);
        assertThat(outbox.getSequence()).isEqualTo(1);
    }

    @Test
    @DisplayName("Registration 결과가 비어 있으면 기존 User 결과로 outbox를 생성한다")
    void fromFallsBackToUserDecision() {
        User user =
                User.builder()
                        .email("student@jnu.ac.kr")
                        .pwd("encoded-password")
                        .userRole(UserRole.USER)
                        .build();
        user.success();
        Registration registration = registration();
        registration.setUser(user);

        EmailOutbox outbox = EmailOutbox.from(registration);

        assertThat(outbox.getResultStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(outbox.getSequence()).isEqualTo(-2);
    }

    private Registration registration() {
        Registration registration =
                Registration.builder()
                        .email("student@jnu.ac.kr")
                        .name("학생")
                        .studentNum("20240001")
                        .carNum("12가3456")
                        .isLight(false)
                        .phoneNum("010-0000-0000")
                        .createdAt(LocalDateTime.of(2026, 6, 25, 10, 0))
                        .isSaved(false)
                        .eventId(10L)
                        .build();
        registration.setId(10L);
        return registration;
    }
}
