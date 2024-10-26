package com.jnu.ticketdomain.domains.captcha.repository;


import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptchaLogRepository extends JpaRepository<CaptchaLog, Long> {
    CaptchaLog findTopByUserIdAndIsSuccessFalseOrderByTimestampDesc(Long userId);
}
