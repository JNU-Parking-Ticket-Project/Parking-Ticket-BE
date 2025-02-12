package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ValidateCaptchaUseCase {

    private final CaptchaHashProcessor captchaHashProcessor;

    @Transactional
    public void execute(String encryptedCode, String answer) {
        Long userId = SecurityUtils.getCurrentUserId();
        captchaHashProcessor.verify(encryptedCode, userId, answer);
    }
}
