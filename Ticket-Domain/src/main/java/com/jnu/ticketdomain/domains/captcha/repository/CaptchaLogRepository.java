package com.jnu.ticketdomain.domains.captcha.repository;


import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaptchaLogRepository extends JpaRepository<CaptchaLog, Long> {
    @Query("SELECT c FROM CaptchaLog c WHERE c.userId = :userId ORDER BY c.timestamp DESC")
    CaptchaLog findLatestByUserId(@Param("userId") Long userId);
}
