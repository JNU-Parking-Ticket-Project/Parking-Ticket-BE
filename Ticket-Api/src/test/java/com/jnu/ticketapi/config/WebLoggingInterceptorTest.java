package com.jnu.ticketapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class WebLoggingInterceptorTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(WebLoggingInterceptor.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;
    private boolean previousAdditive;

    @BeforeEach
    void setUp() {
        previousLevel = logger.getLevel();
        previousAdditive = logger.isAdditive();
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
        logger.setAdditive(previousAdditive);
        appender.stop();
    }

    @Test
    @DisplayName("요청 로그는 기존 인증 정보와 HTTP 상태를 사용한다")
    void logsAuthenticatedUserAndResponseStatus() {
        WebLoggingInterceptor interceptor = new WebLoggingInterceptor(new WebProperties(List.of()));
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/registration/3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(400);
        response.setContentType("application/json");
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken("42", "", Collections.emptyList()));

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(appender.list)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getLevel()).isEqualTo(Level.INFO);
                            assertThat(event.getFormattedMessage())
                                    .contains(
                                            "Method: POST",
                                            "URL: /api/v1/registration/3",
                                            "User: 42",
                                            "Status: 400");
                            assertThat(event.getThrowableProxy()).isNull();
                        });
    }

    @Test
    @DisplayName("로그 제외 경로에서도 MDC를 정리한다")
    void clearsMdcWhenRequestLoggingIsSkipped() {
        WebLoggingInterceptor interceptor =
                new WebLoggingInterceptor(new WebProperties(List.of("/api/actuator/*")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/actuator/health");
        request.setServletPath("/api/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("userId", "42");

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get("userId")).isNull();
        assertThat(appender.list).isEmpty();
    }
}
