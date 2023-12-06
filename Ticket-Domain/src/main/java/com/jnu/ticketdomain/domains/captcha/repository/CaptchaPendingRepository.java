package com.jnu.ticketdomain.domains.captcha.repository;


import com.jnu.ticketdomain.domains.captcha.domain.CaptchaPending;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptchaPendingRepository extends JpaRepository<CaptchaPending, Long> {
    @EntityGraph(attributePaths = {"captcha"})
    Optional<CaptchaPending> findById(Long id);
}
