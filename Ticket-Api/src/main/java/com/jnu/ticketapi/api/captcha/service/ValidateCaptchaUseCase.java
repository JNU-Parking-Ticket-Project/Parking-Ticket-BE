package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ValidateCaptchaUseCase {

    private final CaptchaAdaptor captchaAdaptor;
    private final CaptchaHashProcessor captchaHashProcessor;

    @Transactional
    public void execute(String encryptedCode, String answer) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long captchaId = captchaHashProcessor.verify(encryptedCode, userId);

        Captcha captcha = captchaAdaptor.findById(captchaId);
        if (!captcha.validate(answer)) {
            throw WrongCaptchaAnswerException.EXCEPTION;
        }
    }
}
