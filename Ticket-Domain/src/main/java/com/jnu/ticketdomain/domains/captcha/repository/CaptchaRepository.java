package com.jnu.ticketdomain.domains.captcha.repository;

import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaptchaRepository extends JpaRepository<Captcha, Long> {
    Optional<Captcha> findByImageName(String imageName);
}
