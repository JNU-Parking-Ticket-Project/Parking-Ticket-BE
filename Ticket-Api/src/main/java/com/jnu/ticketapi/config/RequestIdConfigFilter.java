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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        MDC.put(REQUEST_ID_KEY, generateRequestId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String generateRequestId() {
        return IntStream.range(0, 8)
                .mapToObj(i -> String.valueOf((char) ThreadLocalRandom.current().nextInt(48, 123)))
                .filter(
                        ch ->
                                (ch.charAt(0) >= '0' && ch.charAt(0) <= '9')
                                        || (ch.charAt(0) >= 'A' && ch.charAt(0) <= 'Z')
                                        || (ch.charAt(0) >= 'a' && ch.charAt(0) <= 'z'))
                .collect(Collectors.joining());
    }

}
