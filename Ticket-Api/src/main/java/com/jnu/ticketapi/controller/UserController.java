package com.jnu.ticketapi.controller;


import com.jnu.ticketapi.application.port.UserUseCase;
import com.jnu.ticketapi.dto.TokenDto;
import com.jnu.ticketdomain.domain.dto.LoginUserRequestDto;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;

    @PostMapping("/user/login")
    public ResponseEntity<TokenDto> logInUser(
            @RequestBody LoginUserRequestDto loginUserRequestDto, HttpServletResponse response) {
        TokenDto token = userUseCase.login(loginUserRequestDto);
        return ResponseEntity.ok(token);
    }
}
