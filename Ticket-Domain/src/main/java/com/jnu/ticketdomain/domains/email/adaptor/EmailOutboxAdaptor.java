package com.jnu.ticketdomain.domains.email.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.email.domain.EmailOutbox;
import com.jnu.ticketdomain.domains.email.repository.EmailOutboxRepository;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Adaptor
@RequiredArgsConstructor
public class EmailOutboxAdaptor {
    private static final int LAST_ERROR_MAX_LENGTH = 1000;
    private static final String UNKNOWN_MAIL_ERROR = "메일 발송 실패 원인을 확인할 수 없습니다.";

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
    public boolean markFailed(Long id, String lastError, int maxRetryCount) {
        if (maxRetryCount < 1) {
            throw new IllegalArgumentException("maxRetryCount는 1 이상이어야 합니다.");
        }
        LocalDateTime failedAt = LocalDateTime.now();
        String normalizedError = normalizeError(lastError);
        int terminalRetryCount = maxRetryCount - 1;
        if (emailOutboxRepository.markTerminalFailure(
                        id, failedAt, normalizedError, terminalRetryCount, maxRetryCount)
                == 1) {
            return true;
        }
        return emailOutboxRepository.markRetryFailure(
                        id, failedAt, normalizedError, terminalRetryCount)
                == 1;
    }

    @Transactional(readOnly = true)
    public Page<EmailOutbox> findFailedByEventId(Long eventId, Pageable pageable) {
        return emailOutboxRepository.findFailedByEventId(eventId, pageable);
    }

    @Transactional
    public boolean requeueFailed(Long eventId, Long id) {
        return emailOutboxRepository.requeueFailed(eventId, id) == 1;
    }

    private String normalizeError(String error) {
        String normalized = StringUtils.hasText(error) ? error.trim() : UNKNOWN_MAIL_ERROR;
        return normalized.substring(0, Math.min(normalized.length(), LAST_ERROR_MAX_LENGTH));
    }
}
