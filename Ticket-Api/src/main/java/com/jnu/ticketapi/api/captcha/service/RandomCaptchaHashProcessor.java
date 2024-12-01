package com.jnu.ticketapi.api.captcha.service;

import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;

import com.jnu.ticketapi.config.EncryptionProperties;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaLogAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@ConditionalOnProperty(prefix = "encryption", name = "salt-type", havingValue = "random")
@RequiredArgsConstructor
public class RandomCaptchaHashProcessor implements CaptchaHashProcessor {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Encryption encryption;
    private final CaptchaLogAdaptor captchaLogAdaptor;
    private final EncryptionProperties properties;

    @Override
    public HashResult hash(Long captchaId) {
        String iv = generateIv();
        String encryptedData = encryption.encrypt(String.valueOf(captchaId), iv);
        return new HashResult(iv, encryptedData);
    }

    @Override
    public Long verify(String encryptedCode, Long userId) {
        CaptchaLog captchaLog = captchaLogAdaptor.findLatestByUserId(userId);
        String decryptedCaptchaId = encryption.decrypt(encryptedCode, captchaLog.getSalt());

        if (!decryptedCaptchaId.equals(String.valueOf(captchaLog.getCaptchaId()))) {
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
