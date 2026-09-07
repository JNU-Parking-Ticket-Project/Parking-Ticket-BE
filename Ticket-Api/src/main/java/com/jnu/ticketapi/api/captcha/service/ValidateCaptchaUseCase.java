package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.service.vo.CaptchaVerification;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLoadPort;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ValidateCaptchaUseCase {

    private final CaptchaLoadPort captchaLoadPort;
    private final CaptchaHashProcessor captchaHashProcessor;
    private final CaptchaLogPort captchaLogPort;

    @Transactional
    public void execute(String encryptedCode, String answer) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long captchaId = captchaHashProcessor.verify(encryptedCode, userId);

        validateAnswer(captchaId, answer);
    }

    @Transactional(readOnly = true)
    public Long validateWithoutConsume(String encryptedCode, String answer) {
        Long userId = SecurityUtils.getCurrentUserId();
        CaptchaVerification verification =
                captchaHashProcessor.verifyWithoutConsume(encryptedCode, userId);

        validateAnswer(verification.captchaId(), answer);
        return verification.captchaLogId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consume(Long captchaLogId) {
        captchaLogPort.markUsed(captchaLogId);
    }

    private void validateAnswer(Long captchaId, String answer) {
        Captcha captcha = captchaLoadPort.findById(captchaId);
        if (!captcha.validate(answer)) {
            throw WrongCaptchaAnswerException.EXCEPTION;
        }
    }
}
