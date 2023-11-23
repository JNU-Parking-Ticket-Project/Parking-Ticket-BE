package com.jnu.ticketapi.controller;


import com.jnu.ticketapi.application.port.AuthUseCase;
import com.jnu.ticketapi.dto.TokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthUseCase authUseCase;

    @PostMapping("/auth/reissue")
    public ResponseEntity<TokenDto> reIssue(
            @CookieValue(name = "refresh_token") String refreshToken,
            @RequestHeader(name = "Authorization") String accessToken) {
        TokenDto reissuedTokenDto = authUseCase.reissue(accessToken, refreshToken);
        return ResponseEntity.ok(reissuedTokenDto);
    }
}
