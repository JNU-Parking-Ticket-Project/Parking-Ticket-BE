package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.model.response.CaptchaResponse;
import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCaptchaUseCase {

    private final CaptchaAdaptor captchaAdaptor;
    private final CaptchaHashProcessor captchaHashProcessor;
    private final CaptchaLogPort captchaLogPort;

    @Value("${captcha.domain}")
    private String cloudfrontUrl;

    private static final String IMAGE_POSTFIX = ".png";

    @Transactional
    public CaptchaResponse execute() {
        Captcha captcha = captchaAdaptor.findByRandom();
        HashResult result = captchaHashProcessor.hash(captcha.getId());
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
