package com.jnu.ticketapi.security;


import com.jnu.ticketapi.common.swagger.exception.SwaggerException;
import com.jnu.ticketcommon.consts.TicketStatic;
import com.jnu.ticketcommon.exception.AuthenticationNotValidException;
import com.jnu.ticketcommon.helper.SpringEnvironmentHelper;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        if ((isSwaggerRequest(request.getRequestURI())
                && !Boolean.TRUE.equals(springEnvironmentHelper.isProdProfile()))) {
            filterChain.doFilter(request, response);
            return;
        } else if ((isSwaggerRequest(request.getRequestURI()))
                && Boolean.TRUE.equals(springEnvironmentHelper.isProdProfile())) {
            throw SwaggerException.EXCEPTION;
        }
        String bearerToken = request.getHeader("Authorization");
        String accessToken = jwtResolver.extractToken(bearerToken);
        log.info("AccessToken : {}", accessToken);
        if (accessToken == null) {
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
