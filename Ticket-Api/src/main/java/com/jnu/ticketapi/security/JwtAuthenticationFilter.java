package com.jnu.ticketapi.security;


import com.jnu.ticketcommon.exception.InvalidTokenException;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtResolver jwtResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("JwtAuthenticationFilter 작동중");
        try {
            String bearerToken = request.getHeader("Authorization");
            String accessToken = jwtResolver.extractToken(bearerToken);
            log.info("AccessToken : {}", accessToken);
            if (accessToken != null && jwtResolver.accessTokenValidateToken(accessToken)) {
                Authentication authentication = jwtResolver.getAuthentication(accessToken);
                log.info("Authentication : {}", authentication.toString());
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JwtAuthenticationFilter 오류 : {}", e.getMessage());
            throw InvalidTokenException.EXCEPTION;
        }
    }
}
