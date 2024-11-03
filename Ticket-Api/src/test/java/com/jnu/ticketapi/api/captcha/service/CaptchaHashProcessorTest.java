package com.jnu.ticketapi.api.captcha.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.jnu.ticketapi.api.captcha.service.salt.RandomSaltGenerator;
import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Hasher;
import com.jnu.ticketapi.config.EncryptionProperties;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CaptchaHashProcessorTest {
    private final EncryptionProperties properties = new EncryptionProperties(16, "SHA-256");

    private final CaptchaHashProcessor hashProcessor =
            new CaptchaHashProcessor(new RandomSaltGenerator(properties), new Hasher(properties));

    @Test
    @DisplayName("평문을 암호화하면 salt와 해시값이 생성된다")
    void encrypt_ShouldGenerateSaltAndHash() {
        // given
        Long plainText = 12345L;

        // when
        HashResult result = hashProcessor.hash(plainText);

        // then
        assertThat(result.getSalt()).isNotNull();
        assertThat(result.getCaptchaCode()).isNotNull();
        assertDoesNotThrow(() -> Base64.getDecoder().decode(result.getSalt()));
        assertDoesNotThrow(() -> Base64.getDecoder().decode(result.getCaptchaCode()));
    }

    @Test
    @DisplayName("같은 평문이라도 매번 다른 salt로 인해 다른 해시값이 생성된다")
    void encrypt_ShouldGenerateDifferentHashForSamePlaintext() {
        // given
        Long plainText = 12345L;

        // when
        HashResult result1 = hashProcessor.hash(plainText);
        HashResult result2 = hashProcessor.hash(plainText);

        // then
        assertThat(result1.getSalt()).isNotEqualTo(result2.getSalt());
        assertThat(result1.getCaptchaCode()).isNotEqualTo(result2.getCaptchaCode());
    }

    @Test
    @DisplayName("CAPTCHA ID 검증이 정상적으로 동작한다")
    void validateCaptchaId_ShouldWorkCorrectly() {
        // given
        Long captchaId = 12345L;
        HashResult hashResult = hashProcessor.hash(captchaId);
        String encryptedCode = hashResult.getCaptchaCode();
        String salt = hashResult.getSalt();

        // when
        boolean isValid = hashProcessor.verify(encryptedCode, captchaId, salt);
        boolean isInvalid = hashProcessor.verify(encryptedCode, 54321L, salt);

        // then
        assertThat(isValid).isTrue();
        assertThat(isInvalid).isFalse();
    }
}
