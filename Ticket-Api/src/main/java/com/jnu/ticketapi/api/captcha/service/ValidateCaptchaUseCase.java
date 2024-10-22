package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaLogAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ValidateCaptchaUseCase {

    private final Encryption encryption;
    private final CaptchaLogAdaptor captchaLogAdaptor;
    private final CaptchaAdaptor captchaAdaptor;

    @Transactional
    public void execute(String encryptedCode, String answer) {
        Long userId = SecurityUtils.getCurrentUserId();
        CaptchaLog captchaLog = captchaLogAdaptor.findLatestByUserId(userId);

        if (!encryption.validateCaptchaId(
                encryptedCode, captchaLog.getCaptchaId(), captchaLog.getSalt())) {
            throw WrongCaptchaAnswerException.EXCEPTION;
        }

        Captcha captcha = captchaAdaptor.findById(captchaLog.getCaptchaId());
        captcha.validate(answer);
    }
}
