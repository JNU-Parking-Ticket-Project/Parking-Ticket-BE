package com.jnu.ticketdomain.domains.captcha.repository;

import com.jnu.ticketdomain.domains.captcha.domain.CaptchaPending;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaptchaPendingRepository extends JpaRepository<CaptchaPending, Long> {
    @EntityGraph(attributePaths = {"captcha"})
    Optional<CaptchaPending> findById(Long id);
}