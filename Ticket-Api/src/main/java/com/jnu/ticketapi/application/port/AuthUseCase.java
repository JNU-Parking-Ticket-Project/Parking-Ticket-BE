package com.jnu.ticketapi.application.port;


import com.jnu.ticketapi.api.auth.model.request.LoginUserRequest;
import com.jnu.ticketapi.api.auth.model.response.LoginUserResponse;
import com.jnu.ticketapi.api.auth.model.response.LogoutUserResponse;
import com.jnu.ticketapi.api.auth.model.response.ReissueTokenResponse;
import com.jnu.ticketapi.api.auth.model.internal.TokenDto;
import com.jnu.ticketapi.api.user.service.UserUseCase;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketapi.security.JwtResolver;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.exception.BadCredentialException;
import com.jnu.ticketcommon.exception.InvalidTokenException;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketinfrastructure.redis.RedisService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@UseCase
public class AuthUseCase {
    private final JwtResolver jwtResolver;
    private final JwtGenerator jwtGenerator;
    private final RedisService redisService;
    private final UserUseCase userUseCase;
    private static final String SERVER = "Server";

    @Transactional(readOnly = true)
    public boolean validate(String refreshToken) {
        if (!jwtResolver.refreshTokenValidateToken(refreshToken)) {
            throw InvalidTokenException.EXCEPTION; // false = 재로그인
        }
        return true; // true = 재발급
    }

    // 토큰 재발급: validate 메서드가 true 반환할 때만 사용 -> AT, RT 재발급
    @Transactional
    public ReissueTokenResponse reissue(String requestAccessToken, String requestRefreshToken) {

        Authentication accessAuthentication = jwtResolver.getAuthentication(requestAccessToken);
        String accessPrincipal = getPrincipal(requestAccessToken);
        String refreshPrincipal = getPrincipal(requestRefreshToken);
        // AT와 RT의 principal이 다를 경우
        if (!accessPrincipal.equals(refreshPrincipal)) {
            throw InvalidTokenException.EXCEPTION; // -> 재로그인 요청
        }

        String refreshTokenInRedis =
                redisService.getValues("RT(" + SERVER + "):" + refreshPrincipal);
        if (refreshTokenInRedis == null) { // Redis에 저장되어 있는 RT가 없을 경우
            throw InvalidTokenException.EXCEPTION; // -> 재로그인 요청
        }

        // 요청된 RT의 유효성 검사 & Redis에 저장되어 있는 RT와 같은지 비교
        if (!jwtResolver.refreshTokenValidateToken(requestRefreshToken)
                || !refreshTokenInRedis.equals(requestRefreshToken)) {
            redisService.deleteValues("RT(" + SERVER + "):" + refreshPrincipal); // 탈취 가능성 -> 삭제
            throw InvalidTokenException.EXCEPTION; // -> 재로그인 요청
        }

        SecurityContextHolder.getContext().setAuthentication(accessAuthentication);
        String authorities = jwtResolver.getAuthorities(accessAuthentication);

        // 토큰 재발급 및 Redis 업데이트
        redisService.deleteValues("RT(" + SERVER + "):" + refreshPrincipal); // 기존 RT 삭제
        ReissueTokenResponse reissueTokenDto =
                ReissueTokenResponse.builder()
                        .accessToken(
                                jwtGenerator.generateAccessToken(refreshPrincipal, authorities))
                        .refreshToken(
                                jwtGenerator.generateRefreshToken(refreshPrincipal, authorities))
                        .build();
        saveRefreshToken(SERVER, refreshPrincipal, reissueTokenDto.refreshToken());
        return reissueTokenDto;
    }

    // 토큰 발급
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

    // AT로부터 principal 추출
    public String getPrincipal(String requestAccessToken) {
        return jwtResolver.getAuthentication(requestAccessToken).getName();
    }

    @Transactional
    public LogoutUserResponse logout(String requestAccessTokenInHeader) {
        String requestAccessToken = extractToken(requestAccessTokenInHeader);
        String principal = getPrincipal(requestAccessToken);

        // Redis에 저장되어 있는 RT 삭제
        String refreshTokenInRedis = redisService.getValues("RT(" + SERVER + "):" + principal);
        if (refreshTokenInRedis != null) {
            redisService.deleteValues("RT(" + SERVER + "):" + principal);
        }
        return LogoutUserResponse.builder().message(ResponseMessage.SUCCESS_LOGOUT).build();
    }

    @Transactional
    public LoginUserResponse login(LoginUserRequest loginUserRequest) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        Optional<User> userOptional = userUseCase.findByEmail(loginUserRequest.email());
        User user;
        if (userOptional.isEmpty()) {
            user = loginUserRequest.toEntity(loginUserRequest);
            userUseCase.save(user);

        } else {
            user = userOptional.get();
            if (!bCryptPasswordEncoder.matches(loginUserRequest.pwd(), user.getPwd())) {
                throw BadCredentialException.EXCEPTION;
            }
        }
        TokenDto tokenDto = generateToken(SERVER, user.getEmail(), user.getUserRole().getValue());
        log.info("accessToken : " + tokenDto.accessToken());
        log.info("refreshToken : " + tokenDto.refreshToken());
        return LoginUserResponse.builder()
                .accessToken(tokenDto.accessToken())
                .refreshToken(tokenDto.refreshToken())
                .build();
    }

    // "Bearer {AT}"에서 AT 추출
    @Transactional(readOnly = true)
    public String extractToken(String bearerToken) {
        return jwtResolver.extractToken(bearerToken);
    }
}
