package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaCodeException;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLogPort;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "encryption", name = "salt-type", havingValue = "fixed")
@RequiredArgsConstructor
public class FixedCaptchaHashProcessor implements CaptchaHashProcessor {
    private static final String FIXED_IV = Base64.getEncoder().encodeToString(new byte[16]);

    private final Encryption encryption;
    private final CaptchaLogPort captchaLogPort;

    @Override
    public HashResult hash(Long captchaId) {
        String encryptedData = encryption.encrypt(String.valueOf(captchaId), FIXED_IV);
        return new HashResult(FIXED_IV, encryptedData);
    }

    @Override
    public Long verify(String hashedCode, Long userId) {
        CaptchaLog captchaLog = captchaLogPort.findLatestByUserId(userId);
        String decryptedCaptchaId = encryption.decrypt(hashedCode, captchaLog.getSalt()); // getSalt() -> FIXED_IV

        if (!decryptedCaptchaId.equals(String.valueOf(captchaLog.getCaptchaId()))) {
            throw WrongCaptchaCodeException.EXCEPTION;
        }

        captchaLog.markUse();
        return captchaLog.getCaptchaId();
    }
}
