package com.jnu.ticketapi.api.auth.service;


import com.jnu.ticketapi.api.auth.model.internal.TokenDto;
import com.jnu.ticketapi.api.auth.model.request.CheckEmailRequest;
import com.jnu.ticketapi.api.auth.model.request.LoginCouncilRequest;
import com.jnu.ticketapi.api.auth.model.request.LoginUserRequest;
import com.jnu.ticketapi.api.auth.model.response.*;
import com.jnu.ticketapi.api.council.service.CouncilUseCase;
import com.jnu.ticketapi.api.user.service.UserUseCase;
import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketapi.security.JwtGenerator;
import com.jnu.ticketapi.security.JwtResolver;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.consts.TicketStatic;
import com.jnu.ticketcommon.exception.BadCredentialException;
import com.jnu.ticketcommon.exception.InvalidTokenException;
import com.jnu.ticketcommon.exception.NotEqualPrincipalException;
import com.jnu.ticketcommon.exception.NotFoundRefreshTokenException;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.council.exception.AlreadyExistEmailException;
import com.jnu.ticketdomain.domains.council.exception.IsNotCouncilException;
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
    private final CouncilUseCase councilUseCase;
    private final Converter converter;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    // 토큰 재발급: validate 메서드가 true 반환할 때만 사용 -> AT, RT 재발급
    @Transactional
    public ReissueTokenResponse reissue(
            String requestAccessToken, String requestRefreshToken, String email) {

        if (!jwtResolver.refreshTokenValidateToken(requestRefreshToken)) {
            throw InvalidTokenException.EXCEPTION;
        }
        Authentication accessAuthentication = jwtResolver.getAuthentication(requestAccessToken);
        String refreshEmail =
                jwtResolver.parseClaims(requestRefreshToken).get(TicketStatic.EMAIL_KEY).toString();
        // AT와 RT의 principal이 다를 경우
        if (!email.equals(refreshEmail)) {
            throw NotEqualPrincipalException.EXCEPTION; // -> 재로그인 요청
        }

        String refreshTokenInRedis =
                redisService.getValues("RT(" + TicketStatic.SERVER + "):" + email);
        if (refreshTokenInRedis == null) { // Redis에 저장되어 있는 RT가 없을 경우
            redisService.deleteValues("RT(" + TicketStatic.SERVER + "):" + email);
            throw NotFoundRefreshTokenException.EXCEPTION; // -> 재로그인 요청
        }

        SecurityContextHolder.getContext().setAuthentication(accessAuthentication);
        String authorities = jwtResolver.getAuthorities(accessAuthentication);

        // 토큰 재발급 및 Redis 업데이트
        redisService.deleteValues("RT(" + TicketStatic.SERVER + "):" + email); // 기존 RT 삭제
        ReissueTokenResponse reissueTokenDto =
                ReissueTokenResponse.builder()
                        .accessToken(jwtGenerator.generateAccessToken(email, authorities))
                        .refreshToken(jwtGenerator.generateRefreshToken(email, authorities))
                        .build();
        saveRefreshToken(TicketStatic.SERVER, email, reissueTokenDto.refreshToken());
        return reissueTokenDto;
    }

    // 토큰 발급
    /*
    @Transactional을 붙이지 않은 이유는 generateToken을 사용하는 Login 메서드에서
    @Transactional을 붙이고 있어서 self-invocation이 발생하기 때문이다.
    */
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
    /*
    @Transactional을 붙이지 않은 이유는 saveRefreshToken을 사용하는 reissue 메서드에서
    @Transactional을 붙이고 있어서 self-invocation이 발생하기 때문이다.
    */
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
        String refreshTokenInRedis =
                redisService.getValues("RT(" + TicketStatic.SERVER + "):" + principal);
        if (refreshTokenInRedis != null) {
            redisService.deleteValues("RT(" + TicketStatic.SERVER + "):" + principal);
        }
        return LogoutUserResponse.builder().message(ResponseMessage.SUCCESS_LOGOUT).build();
    }

    @Transactional
    public LoginUserResponse login(LoginUserRequest loginUserRequest) {
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
        TokenDto tokenDto =
                generateToken(TicketStatic.SERVER, user.getEmail(), user.getUserRole().getValue());
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

    // 학생회 로그인
    @Transactional
    public LoginCouncilResponse loginCouncil(LoginCouncilRequest loginCouncilRequest) {
        User user = councilUseCase.findByEmail(loginCouncilRequest.email());
        if (!bCryptPasswordEncoder.matches(loginCouncilRequest.pwd(), user.getPwd())) {
            throw BadCredentialException.EXCEPTION;
        }
        if (!user.getUserRole().getValue().equals("COUNCIL")
                && !user.getUserRole().getValue().equals("ADMIN")) {
            throw IsNotCouncilException.EXCEPTION;
        }
        TokenDto tokenDto =
                generateToken(TicketStatic.SERVER, user.getEmail(), user.getUserRole().getValue());
        log.info("accessToken : " + tokenDto.accessToken());
        log.info("refreshToken : " + tokenDto.refreshToken());
        return converter.toLoginCouncilResponseDto(tokenDto);
    }

    @Transactional(readOnly = true)
    public CheckEmailResponse checkEmail(CheckEmailRequest checkEmailRequest) {
        Optional<User> user = userUseCase.findByEmail(checkEmailRequest.email());
        if (user.isPresent()) {
            throw AlreadyExistEmailException.EXCEPTION;
        }
        return CheckEmailResponse.of(ResponseMessage.IS_POSSIBLE_EMAIL);
    }
}
