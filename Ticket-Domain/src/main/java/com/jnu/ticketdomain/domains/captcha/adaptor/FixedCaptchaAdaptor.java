package com.jnu.ticketdomain.domains.captcha.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.exception.NotFoundCaptchaException;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLoadPort;
import com.jnu.ticketdomain.domains.captcha.repository.CaptchaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "encryption", name = "salt-type", havingValue = "fixed")
@Adaptor
public class FixedCaptchaAdaptor implements CaptchaLoadPort {
    private final CaptchaRepository captchaRepository;

    @Override
    public Captcha findByRandom() { // fixed
        return captchaRepository
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> NotFoundCaptchaException.EXCEPTION);
    }

    @Override
    public Captcha findById(long id) {
        return captchaRepository.findById(id).orElseThrow(() -> NotFoundCaptchaException.EXCEPTION);
    }
}
