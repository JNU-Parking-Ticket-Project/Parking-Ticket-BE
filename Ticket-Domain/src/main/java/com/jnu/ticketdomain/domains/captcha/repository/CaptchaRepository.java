package com.jnu.ticketdomain.domains.captcha.repository;


import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CaptchaRepository extends JpaRepository<Captcha, Long> {
    Optional<Captcha> findByImageName(String imageName);

    @Query("select c from Captcha c order by rand()")
    Captcha findByRandom();
}
