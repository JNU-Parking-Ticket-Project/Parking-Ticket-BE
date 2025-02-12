package com.jnu.ticketapi.api.captcha.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketapi.config.EncryptionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FixedCaptchaHashProcessorTest {
    private EncryptionProperties properties;
    private Encryption encryption;
    private FixedCaptchaHashProcessor hashProcessor;

    @BeforeEach
    void setUp() {
        properties =
                new EncryptionProperties(
                        "12345678901234567890123456789012", // key
                        "AES/CBC/PKCS5Padding", // algorithm
                        "AES", // key-spec-algorithm
                        16L // length
                        );
        encryption = new Encryption(properties);
        hashProcessor = new FixedCaptchaHashProcessor(encryption);
    }

    @Nested
    @DisplayName("해시 생성 테스트")
    class HashTest {
        @Test
        @DisplayName("같은 평문은 항상 같은 암호화 값이 생성된다")
        void shouldGenerateSameEncryptedValueForSamePlaintext() {
            // given
            Long plainText = 12345L;

            // when
            HashResult result1 = hashProcessor.hash(plainText);
            HashResult result2 = hashProcessor.hash(plainText);

            // then
            assertThat(result1.getSalt()).isEqualTo(result2.getSalt());
            assertThat(result1.getCaptchaCode()).isEqualTo(result2.getCaptchaCode());
        }

        @Test
        @DisplayName("서로 다른 평문은 서로 다른 암호화 값이 생성된다")
        void shouldGenerateDifferentEncryptedValueForDifferentPlaintext() {
            // given
            Long plainText1 = 12345L;
            Long plainText2 = 54321L;

            // when
            HashResult result1 = hashProcessor.hash(plainText1);
            HashResult result2 = hashProcessor.hash(plainText2);

            // then
            assertThat(result1.getSalt()).isEqualTo(result2.getSalt()); // IV는 동일
            assertThat(result1.getCaptchaCode())
                    .isNotEqualTo(result2.getCaptchaCode()); // 암호화 값은 다름
        }
    }

    @Nested
    @DisplayName("검증 테스트")
    class VerifyTest {
        @Test
        @DisplayName("CAPTCHA 검증이 정상적으로 동작한다")
        void shouldVerifyCorrectly() {
            // given
            Long captchaId = 12345L;
            HashResult hashResult = hashProcessor.hash(captchaId);

            // when
            Long decryptedId = hashProcessor.verify(hashResult.getCaptchaCode(), 1L, "answer");

            // then
            assertThat(decryptedId).isEqualTo(captchaId);
        }
    }
}
