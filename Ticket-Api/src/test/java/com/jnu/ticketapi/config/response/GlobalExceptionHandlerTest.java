package com.jnu.ticketapi.config.response;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jnu.ticketcommon.exception.ErrorResponse;
import com.jnu.ticketcommon.exception.SecurityContextNotFoundException;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;
    private boolean previousAdditive;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        previousLevel = logger.getLevel();
        previousAdditive = logger.isAdditive();
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        appender.start();
        logger.addAppender(appender);
        handler = new GlobalExceptionHandler(new MockEnvironment());
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
        logger.setAdditive(previousAdditive);
        appender.stop();
    }

    @Test
    @DisplayName("재고 소진은 기존 400 응답을 유지하고 스택 없이 기록한다")
    void noStockKeepsClientErrorContractWithoutStackTrace() {
        MockHttpServletRequest request = registrationRequest();

        ResponseEntity<ErrorResponse> response =
                handler.ticketCodeExceptionHandler(NoEventStockLeftException.EXCEPTION, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("EVENT_400_2");
        assertThat(appender.list)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                            assertThat(event.getFormattedMessage())
                                    .contains(
                                            "code=EVENT_400_2",
                                            "method=POST",
                                            "uri=/api/v1/registration/3");
                            assertThat(event.getThrowableProxy()).isNull();
                        });
    }

    @Test
    @DisplayName("서버 오류는 ERROR 레벨과 전체 예외 정보를 유지한다")
    void serverErrorKeepsStackTrace() {
        MockHttpServletRequest request = registrationRequest();

        ResponseEntity<ErrorResponse> response =
                handler.ticketCodeExceptionHandler(
                        SecurityContextNotFoundException.EXCEPTION, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(appender.list)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                            assertThat(event.getThrowableProxy()).isNotNull();
                        });
    }

    private MockHttpServletRequest registrationRequest() {
        return new MockHttpServletRequest("POST", "/api/v1/registration/3");
    }
}
