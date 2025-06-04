package com.jnu.ticketapi.security;


import com.jnu.ticketapi.application.service.CustomUserDetailsService;
import com.jnu.ticketcommon.exception.*;
import com.jnu.ticketcommon.exception.UnsupportedJwtException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.security.Key;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class JwtResolver {
    private final CustomUserDetailsService customUserDetailsService;
    private static final String EMAIL_KEY = "Email";
    private static final String BEARER_TYPE = "Bearer";
    private Key key;

    public JwtResolver(
            @Value("${jwt.secret}") String secretKey,
            CustomUserDetailsService customUserDetailsService) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.customUserDetailsService = customUserDetailsService;
    }

    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get(EMAIL_KEY) == null) {
            throw EmailNotExistException.EXCEPTION;
        }
        String email = claims.get(EMAIL_KEY).toString();
        CustomUserDetails customUserDetails = customUserDetailsService.loadUserByUsername(email);
        MDC.put("userId", customUserDetails.getUsername());
        return new UsernamePasswordAuthenticationToken(
                customUserDetails, "", customUserDetails.getAuthorities());
    }

    public boolean accessTokenValidateToken(String accessToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid Access Token", e);
            throw AccessTokenNotValidException.EXCEPTION;
        } catch (ExpiredJwtException e) {
            log.info("Expired Access Token", e);
            throw AccessTokenExpiredException.EXCEPTION;
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
            throw UnsupportedJwtException.EXCEPTION;
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    public boolean refreshTokenValidateToken(String refreshToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(refreshToken);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid Refresh Token", e);
            throw RefreshTokenNotValidException.EXCEPTION;
        } catch (ExpiredJwtException e) {
            log.info("Expired Refresh Token", e);
            throw RefreshTokenExpiredException.EXCEPTION;
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
            throw UnsupportedJwtException.EXCEPTION;
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    public Claims parseClaims(String Token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(Token).getBody();
        } catch (ExpiredJwtException e) {
            // ???
            return e.getClaims();
        }
    }

    public String extractToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_TYPE)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public String getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}
