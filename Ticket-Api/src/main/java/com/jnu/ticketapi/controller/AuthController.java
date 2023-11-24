package com.jnu.ticketapi.controller;


import com.jnu.ticketapi.application.port.AuthUseCase;
import com.jnu.ticketapi.dto.LoginUserRequestDto;
import com.jnu.ticketapi.dto.LoginUserResponseDto;
import com.jnu.ticketapi.dto.ReissueTokenRequestDto;
import com.jnu.ticketapi.dto.ReissueTokenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthUseCase authUseCase;

    @PostMapping("/auth/login")
    public ResponseEntity<LoginUserResponseDto> logInUser(
            @RequestBody LoginUserRequestDto loginUserRequestDto) {
        LoginUserResponseDto responseDto = authUseCase.login(loginUserRequestDto);
        return ResponseEntity.ok(responseDto);
    }

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

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logOut(@RequestHeader("Authorization") String bearerToken) {
        authUseCase.logout(bearerToken);
        return ResponseEntity.ok("로그아웃이 완료 되었습니다.");
    }
}
