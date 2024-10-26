package com.jnu.ticketapi.api.captcha.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.WithCustomMockUser;
import com.jnu.ticketapi.config.BaseIntegrationTest;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaAdaptor;
import com.jnu.ticketdomain.domains.captcha.adaptor.CaptchaLogAdaptor;
import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class GetCaptchaUseCaseTest extends BaseIntegrationTest {
    @Autowired private GetCaptchaUseCase getCaptchaUseCase;

    @MockBean private CaptchaAdaptor captchaAdaptor;

    @Autowired private CaptchaLogAdaptor captchaLogAdaptor;

    @Test
    @WithCustomMockUser(id = 1L)
    @DisplayName("캡차 이미지 조회 시 로그 정보를 저장한다.")
    void execute_integration() {
        // given
        Long userId = 1L;
        Long captchaId = 1L;
        Captcha captcha = mock(Captcha.class);
        when(captcha.getId()).thenReturn(captchaId);
        when(captchaAdaptor.findByRandom()).thenReturn(captcha);

        // when
        getCaptchaUseCase.execute();

        // then
        assertAll(
                () ->
                        assertEquals(
                                captchaLogAdaptor.findLatestByUserId(userId).getCaptchaId(),
                                captchaId),
                () ->
                        assertEquals(
                                captchaLogAdaptor.findLatestByUserId(userId).getUserId(), userId),
                () -> assertFalse(captchaLogAdaptor.findLatestByUserId(userId).getIsSuccess()));
    }
}
