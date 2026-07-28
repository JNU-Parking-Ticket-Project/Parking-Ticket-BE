package com.jnu.ticketdomain.domains.email.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.email.repository.EmailOutboxRepository;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@RequiredArgsConstructor
public class EmailOutboxAdaptor {
    private final EmailOutboxRepository emailOutboxRepository;

    public void saveRegistrationResultIfAbsent(Registration registration) {
        if (emailOutboxRepository.existsByRegistrationId(registration.getId())) {
            return;
        }
        emailOutboxRepository.save(EmailOutbox.from(registration));
    }

    public List<EmailOutbox> findPending(int size, LocalDateTime staleBefore, int maxRetryCount) {
        return emailOutboxRepository.findPending(
                staleBefore, maxRetryCount, PageRequest.of(0, size));
    }

    @Transactional
    public boolean claim(Long id, LocalDateTime staleBefore, int maxRetryCount) {
        return emailOutboxRepository.claim(id, LocalDateTime.now(), staleBefore, maxRetryCount)
                == 1;
    }

    @Transactional
    public void markSent(Long id) {
        emailOutboxRepository.markSent(id, LocalDateTime.now());
    }

    @Transactional
    public void markFailed(Long id) {
        emailOutboxRepository.markFailed(id, LocalDateTime.now());
    }
}
