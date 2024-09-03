package com.jnu.ticketapi.config;


import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
class ServletWrappingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CacheAccessRequestFilter wrapRequest = new CacheAccessRequestFilter(request);
        ContentCachingResponseWrapper wrapResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(wrapRequest, wrapResponse);
        wrapResponse.copyBodyToResponse();
    }
}
