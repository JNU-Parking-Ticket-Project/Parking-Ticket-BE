package com.jnu.ticketapi.api.captcha.service;


import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;
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

    @Override
    public HashResult hash(Long captchaId) {
        String encryptedData = encryption.encrypt(String.valueOf(captchaId), FIXED_IV);
        return new HashResult(FIXED_IV, encryptedData);
    }

    @Override
    public Long verify(String hashedCode, Long userId, String answer) {
        return Long.parseLong(encryption.decrypt(hashedCode, FIXED_IV));
    }
}
