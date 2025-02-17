package com.jnu.ticketapi.api.captcha.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.WithCustomMockUser;
import com.jnu.ticketapi.config.BaseIntegrationTest;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLoadPort;
import com.jnu.ticketdomain.domains.captcha.out.CaptchaLogPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class GetCaptchaUseCaseTest extends BaseIntegrationTest {
    @Autowired private GetCaptchaUseCase getCaptchaUseCase;

    @MockBean private CaptchaLoadPort captchaLoadPort;

    @Autowired private CaptchaLogPort captchaLogPort;

    @Test
    @WithCustomMockUser(id = 1L)
    @DisplayName("캡차 이미지 조회 시 로그 정보를 저장한다.")
    void execute_integration() {
        // given
        Long userId = 1L;
        Long captchaId = 1L;
        Captcha captcha = mock(Captcha.class);
        when(captcha.getId()).thenReturn(captchaId);
        when(captchaLoadPort.findByRandom()).thenReturn(captcha);

        // when
        getCaptchaUseCase.execute();

        // then
        assertAll(
                () ->
                        assertEquals(
                                captchaLogPort.findLatestByUserId(userId).getCaptchaId(),
                                captchaId),
                () ->
                        assertEquals(
                                captchaLogPort.findLatestByUserId(userId).getUserId(), userId),
                () -> assertFalse(captchaLogPort.findLatestByUserId(userId).getIsSuccess()));
    }
}
