package com.jnu.ticketdomain.domains.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    @DisplayName("Registration 결과가 비어 있으면 User 결과로 대체하지 않고 생성을 거부한다")
    void fromRejectsMissingRegistrationDecision() {
        Registration registration = registration();

        assertThatThrownBy(() -> EmailOutbox.from(registration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("registrationId=10");
    }

    @Test
    @DisplayName("과거 Registration의 eventId가 비어 있으면 Sector가 속한 이벤트를 사용한다")
    void fromResolvesLegacyEventIdFromSector() {
        Event event = Event.builder().title("과거 이벤트").build();
        ReflectionTestUtils.setField(event, "id", 20L);
        Sector sector =
                Sector.builder()
                        .sectorNumber("1구간")
                        .name("공과대학")
                        .sectorCapacity(2)
                        .reserve(1)
                        .build();
        sector.setEvent(event);
        Registration registration = registration(null);
        registration.setSector(sector);
        registration.finalSave(1, UserStatus.SUCCESS, -2);

        EmailOutbox outbox = EmailOutbox.from(registration);

        assertThat(outbox.getEventId()).isEqualTo(20L);
    }

    private Registration registration() {
        return registration(10L);
    }

    private Registration registration(Long eventId) {
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
                        .eventId(eventId)
                        .build();
        registration.setId(10L);
        return registration;
    }
}
