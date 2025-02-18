package com.jnu.ticketapi.api.captcha.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketapi.config.EncryptionProperties;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaCodeException;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLogPort;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RandomCaptchaHashProcessorTest {
    private EncryptionProperties properties;
    private CaptchaLogPort captchaLogPort;
    private Encryption encryption;
    private CaptchaHashProcessor hashProcessor;

    @BeforeEach
    void setUp() {
        properties =
                new EncryptionProperties(
                        "12345678901234567890123456789012", // key
                        "AES/CBC/PKCS5Padding", // algorithm
                        "AES", // key-spec-algorithm
                        16L // length
                        );
        captchaLogPort = mock(CaptchaLogPort.class);
        encryption = new Encryption(properties);
        hashProcessor = new RandomCaptchaHashProcessor(encryption, captchaLogPort, properties);
    }

    @Nested
    @DisplayName("해시 생성 테스트")
    class HashTest {
        @Test
        @DisplayName("평문을 암호화하면 IV(salt)와 암호화된 값이 생성된다")
        void shouldGenerateIvAndEncryptedValue() {
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
        @DisplayName("같은 평문이라도 매번 다른 IV로 인해 다른 암호화 값이 생성된다")
        void shouldGenerateDifferentEncryptedValueForSamePlaintext() {
            // given
            Long plainText = 12345L;

            // when
            HashResult result1 = hashProcessor.hash(plainText);
            HashResult result2 = hashProcessor.hash(plainText);

            // then
            assertThat(result1.getSalt()).isNotEqualTo(result2.getSalt());
            assertThat(result1.getCaptchaCode()).isNotEqualTo(result2.getCaptchaCode());
        }
    }

    @Nested
    @DisplayName("검증 테스트")
    class VerifyTest {
        @Test
        @DisplayName("CAPTCHA 검증이 정상적으로 동작한다")
        void shouldVerifyCorrectly() {
            // given
            Long userId = 1L;
            Long captchaId = 12345L;
            HashResult hashResult = hashProcessor.hash(captchaId);
            String captchaCode = hashResult.getCaptchaCode();

            CaptchaLog captchaLog = mock(CaptchaLog.class);
            when(captchaLog.getCaptchaId()).thenReturn(captchaId);
            when(captchaLog.getSalt()).thenReturn(hashResult.getSalt());
            when(captchaLogPort.findLatestByUserId(userId)).thenReturn(captchaLog);

            // when & then
            assertDoesNotThrow(() -> hashProcessor.verify(captchaCode, userId));
        }

        @Test
        @DisplayName("잘못된 CAPTCHA 코드가 입력되면 예외가 발생한다")
        void shouldThrowExceptionWhenCaptchaCodeMismatch() {
            // given
            Long userId = 1L;
            Long captchaId = 12345L;
            Long wrongCaptchaId = 54321L;

            HashResult hashResult = hashProcessor.hash(wrongCaptchaId);
            String wrongCaptchaCode = hashResult.getCaptchaCode();

            CaptchaLog captchaLog = mock(CaptchaLog.class);
            when(captchaLog.getCaptchaId()).thenReturn(captchaId);
            when(captchaLog.getSalt()).thenReturn(hashResult.getSalt());
            when(captchaLogPort.findLatestByUserId(userId)).thenReturn(captchaLog);

            // when & then
            assertThrows(
                    WrongCaptchaCodeException.class,
                    () -> hashProcessor.verify(wrongCaptchaCode, userId));
        }
    }
}
