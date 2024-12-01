package com.jnu.ticketdomain.domains.captcha.repository;


import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaptchaLogRepository extends JpaRepository<CaptchaLog, Long> {
    Optional<CaptchaLog> findTopByUserIdAndIsSuccessFalseOrderByTimestampDesc(Long userId);
}
