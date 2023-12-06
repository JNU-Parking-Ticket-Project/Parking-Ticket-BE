package com.jnu.ticketdomain.domains.captcha.adaptor;

import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.exception.NotFoundCaptchaException;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLoadPort;
import com.jnu.ticketdomain.domains.captcha.repository.CaptchaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adaptor
public class CaptchaAdaptor implements CaptchaLoadPort {
    private final CaptchaRepository captchaRepository;

    @Override
    public Captcha findByImageName(String imageName) {
        return captchaRepository.findByImageName(imageName)
                .orElseThrow(() -> NotFoundCaptchaException.EXCEPTION);
    }

    @Override
    public Captcha findByRandom() {
        return captchaRepository.findByRandom();
    }
}
