package com.jnu.ticketinfrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.jnu.ticketinfrastructure.model.MailSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import software.amazon.awssdk.services.ses.SesAsyncClient;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock private SesAsyncClient sesAsyncClient;
    @Mock private SpringTemplateEngine templateEngine;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = spy(new MailService(sesAsyncClient, templateEngine));
        ReflectionTestUtils.setField(mailService, "mailAddress", "center@jnu.ac.kr");
        ReflectionTestUtils.setField(mailService, "announcementUrl", "https://example.com");
    }

    @Test
    @DisplayName("SES 성공 응답은 오류가 없는 발송 성공 결과를 반환한다")
    void returnsSuccessWhenSesResponseIsSuccessful() throws Exception {
        doReturn(true)
                .when(mailService)
                .sendMail(anyString(), anyString(), anyString(), any(Context.class));

        MailSendResult result =
                mailService.sendRegistrationResultMail("student@jnu.ac.kr", "학생", "합격", -2);

        assertThat(result.successful()).isTrue();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("SES 비성공 응답은 원인을 포함한 발송 실패 결과를 반환한다")
    void returnsFailureWhenSesResponseIsUnsuccessful() throws Exception {
        doReturn(false)
                .when(mailService)
                .sendMail(anyString(), anyString(), anyString(), any(Context.class));

        MailSendResult result =
                mailService.sendRegistrationResultMail("student@jnu.ac.kr", "학생", "합격", -2);

        assertThat(result.successful()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("SES가 성공 응답을 반환하지 않았습니다.");
    }

    @Test
    @DisplayName("SES 예외는 최하위 원인을 포함한 발송 실패 결과를 반환한다")
    void returnsRootCauseWhenSesRequestFails() throws Exception {
        doThrow(new RuntimeException("wrapper", new IllegalStateException("SES unavailable")))
                .when(mailService)
                .sendMail(anyString(), anyString(), anyString(), any(Context.class));

        MailSendResult result =
                mailService.sendRegistrationResultMail("student@jnu.ac.kr", "학생", "합격", -2);

        assertThat(result.successful()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("IllegalStateException: SES unavailable");
    }
}
