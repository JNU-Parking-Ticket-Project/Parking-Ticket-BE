package com.jnu.ticketapi.api.captcha.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.captcha.service.vo.CaptchaVerification;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLoadPort;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLogPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ValidateCaptchaUseCaseTest {
    private static final Long USER_ID = 1L;

    private CaptchaLoadPort captchaLoadPort;
    private CaptchaHashProcessor captchaHashProcessor;
    private CaptchaLogPort captchaLogPort;
    private ValidateCaptchaUseCase validateCaptchaUseCase;

    @BeforeEach
    void setUp() {
        captchaLoadPort = mock(CaptchaLoadPort.class);
        captchaHashProcessor = mock(CaptchaHashProcessor.class);
        captchaLogPort = mock(CaptchaLogPort.class);
        validateCaptchaUseCase =
                new ValidateCaptchaUseCase(captchaLoadPort, captchaHashProcessor, captchaLogPort);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new TestingAuthenticationToken(
                                String.valueOf(USER_ID), "password", "ROLE_TEST"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("소비 없이 CAPTCHA 코드와 답을 검증하고 로그 ID를 반환한다")
    void validateWithoutConsumeReturnsCaptchaLogId() {
        // given
        String encryptedCode = "encrypted-code";
        String answer = "answer";
        Long captchaId = 10L;
        Long captchaLogId = 20L;
        Captcha captcha = mock(Captcha.class);

        when(captchaHashProcessor.verifyWithoutConsume(encryptedCode, USER_ID))
                .thenReturn(new CaptchaVerification(captchaId, captchaLogId));
        when(captchaLoadPort.findById(captchaId)).thenReturn(captcha);
        when(captcha.validate(answer)).thenReturn(true);

        // when
        Long result = validateCaptchaUseCase.validateWithoutConsume(encryptedCode, answer);

        // then
        assertThat(result).isEqualTo(captchaLogId);
        verifyNoInteractions(captchaLogPort);
    }

    @Test
    @DisplayName("CAPTCHA 답이 틀리면 로그를 소비하지 않는다")
    void validateWithoutConsumeDoesNotConsumeWhenAnswerIsWrong() {
        // given
        String encryptedCode = "encrypted-code";
        String answer = "wrong-answer";
        Long captchaId = 10L;
        Long captchaLogId = 20L;
        Captcha captcha = mock(Captcha.class);

        when(captchaHashProcessor.verifyWithoutConsume(encryptedCode, USER_ID))
                .thenReturn(new CaptchaVerification(captchaId, captchaLogId));
        when(captchaLoadPort.findById(captchaId)).thenReturn(captcha);
        when(captcha.validate(answer)).thenReturn(false);

        // when & then
        assertThatThrownBy(
                        () -> validateCaptchaUseCase.validateWithoutConsume(encryptedCode, answer))
                .isInstanceOf(WrongCaptchaAnswerException.class);
        verify(captchaLogPort, never()).markUsed(captchaLogId);
    }

    @Test
    @DisplayName("검증된 CAPTCHA 로그를 ID로 소비한다")
    void consumeMarksCaptchaLogUsed() {
        // given
        Long captchaLogId = 20L;

        // when
        validateCaptchaUseCase.consume(captchaLogId);

        // then
        verify(captchaLogPort).markUsed(captchaLogId);
    }

    @Test
    @DisplayName("기존 검증 API는 CAPTCHA를 즉시 소비하는 동작을 유지한다")
    void executeKeepsImmediateConsumeBehavior() {
        // given
        String encryptedCode = "encrypted-code";
        String answer = "answer";
        Long captchaId = 10L;
        Captcha captcha = mock(Captcha.class);

        when(captchaHashProcessor.verify(encryptedCode, USER_ID)).thenReturn(captchaId);
        when(captchaLoadPort.findById(captchaId)).thenReturn(captcha);
        when(captcha.validate(answer)).thenReturn(true);

        // when
        validateCaptchaUseCase.execute(encryptedCode, answer);

        // then
        verify(captchaHashProcessor).verify(encryptedCode, USER_ID);
        verify(captchaHashProcessor, never()).verifyWithoutConsume(encryptedCode, USER_ID);
    }
}
