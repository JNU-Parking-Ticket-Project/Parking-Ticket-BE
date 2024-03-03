package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.model.response.CaptchaResponse;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetCaptchaUseCase {

    private final CaptchaAdaptor captchaAdaptor;
    private final Encryption encryption;

    @Value("${captcha.domain}")
    private String cloudfrontUrl;
    private static final String IMAGE_POSTFIX = ".png";

    @Transactional
    public CaptchaResponse execute() {
        Captcha captcha = captchaAdaptor.findByRandom();
        String code = encryption.encrypt(captcha.getId());
        return CaptchaResponse.of(cloudfrontUrl, IMAGE_POSTFIX, code, captcha);
    }
}
