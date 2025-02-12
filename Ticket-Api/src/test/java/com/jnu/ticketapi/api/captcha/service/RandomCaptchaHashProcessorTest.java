package com.jnu.ticketapi.api.captcha.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.captcha.service.vo.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketapi.config.EncryptionProperties;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaLogAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaCodeException;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RandomCaptchaHashProcessorTest {
    private EncryptionProperties properties;
    private CaptchaLogAdaptor captchaLogAdaptor;
    private Encryption encryption;
    private CaptchaHashProcessor hashProcessor;
    private CaptchaAdaptor captchaAdaptor;

    @BeforeEach
    void setUp() {
        properties =
                new EncryptionProperties(
                        "12345678901234567890123456789012", // key
                        "AES/CBC/PKCS5Padding", // algorithm
                        "AES", // key-spec-algorithm
                        16L // length
                        );
        captchaLogAdaptor = mock(CaptchaLogAdaptor.class);
        captchaAdaptor = mock(CaptchaAdaptor.class);
        encryption = new Encryption(properties);
        hashProcessor = new RandomCaptchaHashProcessor(encryption, captchaLogAdaptor, properties, captchaAdaptor);
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
            String answer = "answer";
            HashResult hashResult = hashProcessor.hash(captchaId);
            String captchaCode = hashResult.getCaptchaCode();

            CaptchaLog captchaLog = mock(CaptchaLog.class);
            when(captchaLog.getCaptchaId()).thenReturn(captchaId);
            when(captchaLog.getSalt()).thenReturn(hashResult.getSalt());

            Captcha captcha = mock(Captcha.class);
            when(captcha.getAnswer()).thenReturn(answer);
            when(captcha.validate(anyString())).thenAnswer(invocation ->
                    answer.equals(invocation.getArgument(0, String.class)));

            when(captchaAdaptor.findById(anyLong())).thenReturn(captcha);
            when(captchaLogAdaptor.findLatestByUserId(userId)).thenReturn(captchaLog);

            // when & then
            assertDoesNotThrow(() -> hashProcessor.verify(captchaCode, userId, answer));
        }

        @Test
        @DisplayName("잘못된 CAPTCHA 답변이 입력되면 예외가 발생한다")
        void shouldThrowExceptionWhenAnswerMismatch() {
            //given
            Long userId = 1L;
            Long captchaId = 12345L;
            String answer = "answer";
            HashResult hashResult = hashProcessor.hash(captchaId);
            String captchaCode = hashResult.getCaptchaCode();

            CaptchaLog captchaLog = mock(CaptchaLog.class);
            when(captchaLog.getCaptchaId()).thenReturn(captchaId);
            when(captchaLog.getSalt()).thenReturn(hashResult.getSalt());

            Captcha captcha = mock(Captcha.class);
            when(captcha.getAnswer()).thenReturn(answer);
            when(captcha.validate(anyString())).thenAnswer(invocation ->
                    answer.equals(invocation.getArgument(0, String.class)));

            when(captchaAdaptor.findById(anyLong())).thenReturn(captcha);
            when(captchaLogAdaptor.findLatestByUserId(userId)).thenReturn(captchaLog);

            // when & then
            assertThrows(
                    WrongCaptchaAnswerException.class,
                    () -> hashProcessor.verify(captchaCode, userId, "wrong answer"));
        }

        @Test
        @DisplayName("잘못된 CAPTCHA 코드가 입력되면 예외가 발생한다")
        void shouldThrowExceptionWhenCaptchaCodeMismatch() {
            // given
            Long userId = 1L;
            Long captchaId = 12345L;
            Long wrongCaptchaId = 54321L;
            String answer = "answer";

            HashResult hashResult = hashProcessor.hash(captchaId);
            String captchaCode = hashResult.getCaptchaCode();

            CaptchaLog captchaLog = mock(CaptchaLog.class);
            when(captchaLog.getCaptchaId()).thenReturn(wrongCaptchaId);
            when(captchaLog.getSalt()).thenReturn(hashResult.getSalt());
            when(captchaLogAdaptor.findLatestByUserId(userId)).thenReturn(captchaLog);

            // when & then
            assertThrows(
                    WrongCaptchaCodeException.class,
                    () -> hashProcessor.verify(captchaCode, userId, answer));
        }
    }
}
