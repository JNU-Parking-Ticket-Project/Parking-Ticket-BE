package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketapi.config.EncryptionProperties;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaLogAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaCodeException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "encryption", name = "salt-type", havingValue = "random")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RandomCaptchaHashProcessor implements CaptchaHashProcessor {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Encryption encryption;
    private final CaptchaLogAdaptor captchaLogAdaptor;
    private final EncryptionProperties properties;
    private final CaptchaAdaptor captchaAdaptor;

    @Override
    public HashResult hash(Long captchaId) {
        String iv = generateIv();
        String encryptedData = encryption.encrypt(String.valueOf(captchaId), iv);
        return new HashResult(iv, encryptedData);
    }

    @Transactional
    @Override
    public Long verify(String encryptedCode, Long userId, String answer) {
        CaptchaLog captchaLog = captchaLogAdaptor.findLatestByUserId(userId);
        String decryptedCaptchaId = encryption.decrypt(encryptedCode, captchaLog.getSalt());

        if (!decryptedCaptchaId.equals(String.valueOf(captchaLog.getCaptchaId()))) {
            throw WrongCaptchaCodeException.EXCEPTION;
        }

        Captcha captcha = captchaAdaptor.findById(captchaLog.getCaptchaId());
        if (!captcha.validate(answer)) {
            throw WrongCaptchaAnswerException.EXCEPTION;
        }

        captchaLog.markUse();
        return captchaLog.getCaptchaId();
    }

    private String generateIv() {
        byte[] iv = new byte[properties.getLength().intValue()];
        RANDOM.nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }
}
