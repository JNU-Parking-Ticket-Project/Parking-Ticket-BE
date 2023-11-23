package com.jnu.ticketapi.application.service;


import com.jnu.ticketapi.application.port.AuthUseCase;
import com.jnu.ticketapi.dto.TokenDto;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketapi.security.JwtResolver;
import com.jnu.ticketcommon.exception.InvalidTokenException;
import com.jnu.ticketinfrastructure.redis.RedisService;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final JwtResolver jwtResolver;
    private final JwtGenerator jwtGenerator;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RedisService redisService;

    private static final String SERVER = "Server";

    // AT가 만료일자만 초과한 유효한 토큰인지 검사
    @Override
    public boolean validate(String requestAccessTokenInHeader) {
        String requestAccessToken = resolveToken(requestAccessTokenInHeader);
        return jwtResolver.accessTokenValidateToken(requestAccessToken); // true = 재발급
    }

    // 토큰 재발급: validate 메서드가 true 반환할 때만 사용 -> AT, RT 재발급
    @Override
    @Transactional
    public TokenDto reissue(String requestAccessTokenInHeader, String requestRefreshToken) {
        String requestAccessToken = resolveToken(requestAccessTokenInHeader);

        Authentication authentication = jwtResolver.getAuthentication(requestAccessToken);
        String principal = getPrincipal(requestAccessToken);

        String refreshTokenInRedis = redisService.getValues("RT(" + SERVER + "):" + principal);
        if (refreshTokenInRedis == null) { // Redis에 저장되어 있는 RT가 없을 경우
            throw InvalidTokenException.EXCEPTION; // -> 재로그인 요청
        }

        // 요청된 RT의 유효성 검사 & Redis에 저장되어 있는 RT와 같은지 비교
        if (!jwtResolver.refreshTokenValidateToken(requestRefreshToken)
                || !refreshTokenInRedis.equals(requestRefreshToken)) {
            redisService.deleteValues("RT(" + SERVER + "):" + principal); // 탈취 가능성 -> 삭제
            throw InvalidTokenException.EXCEPTION; // -> 재로그인 요청
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String authorities = jwtResolver.getAuthorities(authentication);

        // 토큰 재발급 및 Redis 업데이트
        redisService.deleteValues("RT(" + SERVER + "):" + principal); // 기존 RT 삭제
        TokenDto tokenDto =
                TokenDto.builder()
                        .accessToken(jwtGenerator.generateAccessToken(principal, authorities))
                        .refreshToken(jwtGenerator.generateRefreshToken(principal, authorities))
                        .build();
        saveRefreshToken(SERVER, principal, tokenDto.refreshToken());
        return tokenDto;
    }

    // 토큰 발급
    @Override
    @Transactional
    public TokenDto generateToken(String provider, String email, String authorities) {
        // RT가 이미 있을 경우
        if (redisService.getValues("RT(" + provider + "):" + email) != null) {
            redisService.deleteValues("RT(" + provider + "):" + email); // 삭제
        }

        // AT, RT 생성 및 Redis에 RT 저장
        TokenDto tokenDto =
                TokenDto.builder()
                        .accessToken(jwtGenerator.generateAccessToken(email, authorities))
                        .refreshToken(jwtGenerator.generateRefreshToken(email, authorities))
                        .build();
        saveRefreshToken(provider, email, tokenDto.refreshToken());
        return tokenDto;
    }

    // RT를 Redis에 저장
    @Override
    @Transactional
    public void saveRefreshToken(String provider, String principal, String refreshToken) {
        redisService.setValuesWithTimeout(
                "RT(" + provider + "):" + principal, // key
                refreshToken, // value
                jwtResolver
                        .parseClaims(refreshToken)
                        .getExpiration()
                        .getTime()); // timeout(milliseconds)
    }

    // 권한 이름 가져오기

    // AT로부터 principal 추출
    private String getPrincipal(String requestAccessToken) {
        return jwtResolver.getAuthentication(requestAccessToken).getName();
    }

    // "Bearer {AT}"에서 AT 추출
    @Override
    public String resolveToken(String requestAccessTokenInHeader) {
        if (requestAccessTokenInHeader != null
                && requestAccessTokenInHeader.startsWith("Bearer ")) {
            return requestAccessTokenInHeader.substring(7);
        }
        return null;
    }

    @Override
    @Transactional
    public void logout(String requestAccessTokenInHeader) {
        String requestAccessToken = resolveToken(requestAccessTokenInHeader);
        String principal = getPrincipal(requestAccessToken);

        // Redis에 저장되어 있는 RT 삭제
        String refreshTokenInRedis = redisService.getValues("RT(" + SERVER + "):" + principal);
        if (refreshTokenInRedis != null) {
            redisService.deleteValues("RT(" + SERVER + "):" + principal);
        }
    }
}
