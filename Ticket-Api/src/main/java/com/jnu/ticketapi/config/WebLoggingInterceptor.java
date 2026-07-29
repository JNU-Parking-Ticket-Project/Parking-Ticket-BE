package com.jnu.ticketapi.config;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebLoggingInterceptor implements HandlerInterceptor {
    private static final String START_TIME_ATTR_NAME = "startTime";

    private final WebProperties webProperties;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR_NAME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        try {
            if (!isSkipLogging(request)) {
                createRequestLog(request, response);
            }
        } finally {
            MDC.clear();
        }
    }

    private boolean isSkipLogging(HttpServletRequest request) {
        return webProperties.isNoLoggable(request.getServletPath()) || isPreflight(request);
    }

    private boolean isPreflight(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    private void createRequestLog(HttpServletRequest request, HttpServletResponse response) {
        String currentUserId = getCurrentUserId();
        long executionTime = getExecutionTime(request);
        String requestUrl = request.getRequestURI();
        String method = request.getMethod();
        String responseType = response.getContentType();
        int responseStatus = response.getStatus();

        log.info(
                "Method: {}, URL: {}, User: {}, Status: {}, ResponseType: {}, ResponseTime: {}ms",
                method,
                requestUrl,
                currentUserId,
                responseStatus,
                responseType,
                executionTime);
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private long getExecutionTime(HttpServletRequest request) {
        long startTime = (long) request.getAttribute(START_TIME_ATTR_NAME);
        return System.currentTimeMillis() - startTime;
    }
}
