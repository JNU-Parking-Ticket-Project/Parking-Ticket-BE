package com.jnu.ticketapi.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jnu.ticketapi.common.swagger.exception.SwaggerErrorCode;
import com.jnu.ticketapi.common.swagger.exception.SwaggerException;
import com.jnu.ticketcommon.consts.TicketStatic;
import com.jnu.ticketcommon.exception.AuthenticationNotValidException;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jnu.ticketcommon.exception.ErrorResponse;
import com.jnu.ticketcommon.helper.SpringEnvironmentHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtResolver jwtResolver;
    private final SpringEnvironmentHelper springEnvironmentHelper;
    private AntPathMatcher antPathMatcher = new AntPathMatcher();


    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("JwtAuthenticationFilter 작동중");
        log.info("requestURI : {}", request.getRequestURI());
        if(Boolean.TRUE.equals(springEnvironmentHelper.isProdProfile()) && (isSwaggerRequest(request.getRequestURI()))) {
                filterChain.doFilter(request, response);
                return;
        }
        else if(!springEnvironmentHelper.isProdProfile() && (isSwaggerRequest(request.getRequestURI()))) {
            throw SwaggerException.EXCEPTION;
        }
        String bearerToken = request.getHeader("Authorization");
        String accessToken = jwtResolver.extractToken(bearerToken);
        log.info("AccessToken : {}", accessToken);
        if(accessToken == null) {
            throw AuthenticationNotValidException.EXCEPTION;
        }
        if (jwtResolver.accessTokenValidateToken(accessToken)) {
            Authentication authentication = jwtResolver.getAuthentication(accessToken);
            log.info("Authentication : {}", authentication.toString());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
    private boolean isSwaggerRequest(String uri) {
        return Arrays.stream(TicketStatic.SwaggerPatterns)
                .anyMatch(pattern -> antPathMatcher.match(pattern, uri));
    }
}


