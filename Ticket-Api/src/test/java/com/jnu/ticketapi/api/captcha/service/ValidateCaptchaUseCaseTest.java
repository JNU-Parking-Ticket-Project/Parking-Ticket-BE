package com.jnu.ticketapi.api.captcha.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.jnu.ticketapi.WithCustomMockUser;
import com.jnu.ticketapi.application.HashResult;
import com.jnu.ticketapi.application.helper.Encryption;
import com.jnu.ticketapi.config.BaseIntegrationTest;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaLogAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.domain.CaptchaLog;
import com.jnu.ticketdomain.domains.captcha.exception.WrongCaptchaAnswerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ValidateCaptchaUseCaseTest extends BaseIntegrationTest {

    @Autowired private ValidateCaptchaUseCase validateCaptchaUseCase;

    @Autowired private Encryption encryption;

    @MockBean private CaptchaLogAdaptor captchaLogAdaptor;

    @MockBean private CaptchaAdaptor captchaAdaptor;

    private static final Long CAPTCHA_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final String ANSWER = "answer";

    private CaptchaLog createCaptchaLog(String salt) {
        CaptchaLog captchaLog = mock(CaptchaLog.class);
        when(captchaLog.getCaptchaId()).thenReturn(CAPTCHA_ID);
        when(captchaLog.getSalt()).thenReturn(salt);
        return captchaLog;
    }

    private Captcha createCaptcha(String answer) {
        return spy(Captcha.builder().answer(answer).build());
    }

    @Test
    @DisplayName("캡차 코드가 사용자에게 제공한 캡차코드와 일치하며 답변이 일치할 때 캡차 검증은 성공한다.")
    @WithCustomMockUser(id = 1L)
    void validateCaptcha_Success() {
        // given
        HashResult result = encryption.encrypt(CAPTCHA_ID);
        Captcha captcha = createCaptcha(ANSWER);
        CaptchaLog captchaLog = createCaptchaLog(result.getSalt());

        given(captchaLogAdaptor.findLatestByUserId(USER_ID)).willReturn(captchaLog);
        given(captchaAdaptor.findById(CAPTCHA_ID)).willReturn(captcha);

        // when & then
        assertDoesNotThrow(() -> validateCaptchaUseCase.execute(result.getCaptchaCode(), ANSWER));
    }

    @Test
    @DisplayName("캡차 코드가 사용자에게 제공한 캡차코드와 일치하지 않을 때 캡차 검증은 실패한다.")
    @WithCustomMockUser(id = 1L)
    void validateCaptcha_WrongEncryptedCode_ThrowsException() {
        // given
        HashResult result = encryption.encrypt(CAPTCHA_ID);
        CaptchaLog captchaLog = createCaptchaLog(result.getSalt());
        String wrongCaptchaCode = "wrongCode";

        given(captchaLogAdaptor.findLatestByUserId(USER_ID)).willReturn(captchaLog);

        // when & then
        assertThrows(
                WrongCaptchaAnswerException.class,
                () -> validateCaptchaUseCase.execute(wrongCaptchaCode, ANSWER));
    }

    @Test
    @DisplayName("캡차 답변이 일치하지 않을 때 캡차 검증은 실패한다.")
    @WithCustomMockUser(id = 1L)
    void validateCaptcha_WrongAnswer_ThrowsException() {
        // given
        HashResult result = encryption.encrypt(CAPTCHA_ID);
        CaptchaLog captchaLog = createCaptchaLog(result.getSalt());
        String captchaAnswer = "differentAnswer";
        Captcha captcha = createCaptcha(captchaAnswer);

        given(captchaLogAdaptor.findLatestByUserId(USER_ID)).willReturn(captchaLog);
        given(captchaAdaptor.findById(CAPTCHA_ID)).willReturn(captcha);

        // when & then
        assertThrows(
                WrongCaptchaAnswerException.class,
                () -> validateCaptchaUseCase.execute(result.getCaptchaCode(), ANSWER));
    }
}
