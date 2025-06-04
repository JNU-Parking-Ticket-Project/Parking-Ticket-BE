package com.jnu.ticketapi.config;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RequestIdConfigFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_KEY = "requestId";

    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        MDC.put(REQUEST_ID_KEY, generateRequestId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    public String generateRequestId() {
        return IntStream.range(0, 8)
                .mapToObj(i -> String.valueOf(CHAR_POOL.charAt(ThreadLocalRandom.current().nextInt(CHAR_POOL.length()))))
                .collect(Collectors.joining());
    }

}
