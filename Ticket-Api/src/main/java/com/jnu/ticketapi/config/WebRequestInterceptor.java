package com.jnu.ticketapi.config;


import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebRequestInterceptor implements HandlerInterceptor {
    private static final String START_TIME_ATTR_NAME = "startTime";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REGISTRATION_PATH = "/registration";

    private final WebProperties webProperties;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        MDC.put(REQUEST_ID_KEY, generateRequestId());
        request.setAttribute(START_TIME_ATTR_NAME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        if (isSkipLogging(request)) {
            return;
        }

        StringBuilder logMessage = createBaseLogMessage(request, response);
        appendSensitiveDataToLogMessage(request, logMessage);

        log.info(logMessage.toString());
        MDC.clear();
    }

    private String generateRequestId() {
        return IntStream.range(0, 8)
                .mapToObj(i -> String.valueOf((char) ThreadLocalRandom.current().nextInt(48, 123)))
                .filter(
                        ch ->
                                (ch.charAt(0) <= '9' || ch.charAt(0) >= 'A')
                                        && (ch.charAt(0) <= 'Z' || ch.charAt(0) >= 'a'))
                .collect(Collectors.joining());
    }

    private boolean isSkipLogging(HttpServletRequest request) {
        return webProperties.isNoLoggable(request.getServletPath()) || isPreflight(request);
    }

    private boolean isPreflight(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    private StringBuilder createBaseLogMessage(
            HttpServletRequest request, HttpServletResponse response) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        long executionTime = getExecutionTime(request);
        String requestUrl = request.getRequestURI();
        String responseType = response.getContentType();

        return new StringBuilder(
                String.format(
                        "URL: %s, User: %s, ResponseType: %s, ResponseTime: %dms",
                        requestUrl, currentUserId, responseType, executionTime));
    }

    private void appendSensitiveDataToLogMessage(
            HttpServletRequest request, StringBuilder logMessage) {
        String requestUri = request.getRequestURI();
        if (requestUri.contains(REGISTRATION_PATH)) {
            appendRegistrationDataToLogMessage(request, logMessage);
        }
    }

    private void appendRegistrationDataToLogMessage(
            HttpServletRequest request, StringBuilder logMessage) {
        appendParameterToLogMessage(
                logMessage, request, "phoneNumber", "Registration data - Phone");
        appendParameterToLogMessage(logMessage, request, "carNumber", "Car");
    }

    private void appendParameterToLogMessage(
            StringBuilder logMessage, HttpServletRequest request, String paramName, String label) {
        Optional.ofNullable(request.getParameter(paramName))
                .ifPresent(
                        value -> logMessage.append(" ").append(label).append(": ").append(value));
    }

    private long getExecutionTime(HttpServletRequest request) {
        long startTime = (long) request.getAttribute(START_TIME_ATTR_NAME);
        return System.currentTimeMillis() - startTime;
    }
}
