package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.model.response.CaptchaResponse;
import com.jnu.ticketapi.application.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLogPort;
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
    private final CaptchaLogPort captchaLogPort;

    @Value("${captcha.domain}")
    private String cloudfrontUrl;

    private static final String IMAGE_POSTFIX = ".png";

    @Transactional
    public CaptchaResponse execute() {
        Captcha captcha = captchaAdaptor.findByRandom();
        HashResult result = encryption.encrypt(captcha.getId());
        Long userId = SecurityUtils.getCurrentUserId();

        captchaLogPort.save(
                CaptchaLog.builder()
                        .captchaId(captcha.getId())
                        .userId(userId)
                        .salt(result.getSalt())
                        .build());

        return CaptchaResponse.of(cloudfrontUrl, IMAGE_POSTFIX, result.getCaptchaCode(), captcha);
    }
}
