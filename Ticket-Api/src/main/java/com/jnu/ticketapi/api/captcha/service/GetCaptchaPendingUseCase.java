package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.model.response.CaptchaPendingResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaPendingAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaPending;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetCaptchaPendingUseCase {

    private final CaptchaAdaptor captchaAdaptor;
    private final CaptchaPendingAdaptor captchaPendingAdaptor;

    @Transactional
    public CaptchaPendingResponse execute() {
        Captcha captcha = captchaAdaptor.findByRandom();
        CaptchaPending captchaPending = new CaptchaPending(captcha);
        return CaptchaPendingResponse.of(captchaPendingAdaptor.save(captchaPending));
    }
}
