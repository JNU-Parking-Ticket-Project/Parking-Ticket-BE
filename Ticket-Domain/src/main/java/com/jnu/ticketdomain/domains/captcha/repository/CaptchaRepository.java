package com.jnu.ticketdomain.domains.captcha.repository;


import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaptchaRepository extends JpaRepository<Captcha, Long> {

    Optional<Captcha> findById(long id);

    Optional<Captcha> findFirstByOrderByIdAsc();

    @Query(value = "select * from captcha_tb LIMIT 1 OFFSET :offset", nativeQuery = true)
    Captcha findOneByOffset(@Param("offset") long offset);
}
