package com.jnu.ticketdomain.domains.email;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.email.repository.EmailOutboxRepository;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailOutboxAdaptorTest {

    @Mock private EmailOutboxRepository emailOutboxRepository;

    private EmailOutboxAdaptor emailOutboxAdaptor;

    @BeforeEach
    void setUp() {
        emailOutboxAdaptor = new EmailOutboxAdaptor(emailOutboxRepository);
    }

    @Test
    @DisplayName("이미 outbox가 있는 registration은 중복 저장하지 않는다")
    void saveRegistrationResultIfAbsentSkipsDuplicateRegistration() {
        Registration registration = registration();
        when(emailOutboxRepository.existsByRegistrationId(10L)).thenReturn(true);

        emailOutboxAdaptor.saveRegistrationResultIfAbsent(registration);

        verify(emailOutboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("outbox가 없는 registration은 결과 메일 outbox를 저장한다")
    void saveRegistrationResultIfAbsentSavesNewOutbox() {
        Registration registration = registration();
        when(emailOutboxRepository.existsByRegistrationId(10L)).thenReturn(false);

        emailOutboxAdaptor.saveRegistrationResultIfAbsent(registration);

        verify(emailOutboxRepository).save(org.mockito.ArgumentMatchers.any(EmailOutbox.class));
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
        registration.finalSave(1, UserStatus.SUCCESS, -2);
        return registration;
    }
}
