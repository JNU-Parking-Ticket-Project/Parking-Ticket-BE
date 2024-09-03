package com.jnu.ticketapi.config;


import com.jnu.ticketapi.security.JwtResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
public class WebLogginInterceptor implements HandlerInterceptor {
    private static final String START_TIME_ATTR_NAME = "startTime";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REGISTRATION_PATH = "/api/v1/registration/";
    private final WebProperties webProperties;
    private final JwtResolver jwtResolver;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        MDC.put(REQUEST_ID_KEY, generateRequestId());
        request.setAttribute(START_TIME_ATTR_NAME, System.currentTimeMillis());

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws IOException {
        if (isSkipLogging(request)) {
            return;
        }

        createBaseLogMessage(request, response);
        appendSensitiveDataToLogMessage(request);

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

    private void createBaseLogMessage(HttpServletRequest request, HttpServletResponse response) {
        String bearerToken = request.getHeader("Authorization");
        String accessToken = jwtResolver.extractToken(bearerToken);

        String currentUserId = jwtResolver.getAuthentication(accessToken).getName();
        long executionTime = getExecutionTime(request);
        String requestUrl = request.getRequestURI();
        String responseType = response.getContentType();

        log.info(
                String.format(
                        "URL: %s, User: %s, ResponseType: %s, ResponseTime: %dms",
                        requestUrl, currentUserId, responseType, executionTime));
    }

    private void appendSensitiveDataToLogMessage(HttpServletRequest request) throws IOException {
        String requestUri = request.getRequestURI();
        if (requestUri.trim().startsWith(REGISTRATION_PATH)) {
            log.info("[registration request]" + getBody(request));
        }
    }

    private String getBody(HttpServletRequest request) throws IOException {
        CacheAccessRequestFilter wrapRequest = (CacheAccessRequestFilter) request;
        byte[] contents = wrapRequest.getContents();
        return new String(contents, StandardCharsets.UTF_8);
    }

    private long getExecutionTime(HttpServletRequest request) {
        long startTime = (long) request.getAttribute(START_TIME_ATTR_NAME);
        return System.currentTimeMillis() - startTime;
    }
}
