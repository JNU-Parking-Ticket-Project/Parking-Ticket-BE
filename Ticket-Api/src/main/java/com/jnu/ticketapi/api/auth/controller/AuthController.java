package com.jnu.ticketapi.api.auth.controller;


import com.jnu.ticketapi.api.auth.model.request.LoginUserRequestDto;
import com.jnu.ticketapi.api.auth.model.request.ReissueTokenRequestDto;
import com.jnu.ticketapi.api.auth.model.response.LoginUserResponseDto;
import com.jnu.ticketapi.api.auth.model.response.LogoutUserResponseDto;
import com.jnu.ticketapi.api.auth.model.response.ReissueTokenResponseDto;
import com.jnu.ticketapi.application.port.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "1. [인증]")
public class AuthController {
    private final AuthUseCase authUseCase;

    @Operation(summary = "로그인/회원가입", description = "로그인을 하면 동시에 회원가입이 되면서 로그인 처리")
    @PostMapping("/auth/login")
    public ResponseEntity<LoginUserResponseDto> logInUser(
            @RequestBody LoginUserRequestDto loginUserRequestDto) {
        LoginUserResponseDto responseDto = authUseCase.login(loginUserRequestDto);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "AccessToken 재발급(만료된 AccessToken과 RefreshToken의 principal이 같으면)")
    @PostMapping("/auth/reissue")
    public ResponseEntity<ReissueTokenResponseDto> reIssue(
            @RequestBody ReissueTokenRequestDto requestDto,
            @RequestHeader("Authorization") String bearerToken) {
        String accessToken = authUseCase.extractToken(bearerToken);
        authUseCase.validate(requestDto.refreshToken());
        ReissueTokenResponseDto responseDto =
                authUseCase.reissue(accessToken, requestDto.refreshToken());
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "로그아웃", description = "로그아웃(redis에 저장된 RefreshToken을 삭제)")
    @PostMapping("/auth/logout")
    public ResponseEntity<LogoutUserResponseDto> logOut(
            @RequestHeader("Authorization") String bearerToken) {
        LogoutUserResponseDto responseDto = authUseCase.logout(bearerToken);
        return ResponseEntity.ok(responseDto);
    }
}
