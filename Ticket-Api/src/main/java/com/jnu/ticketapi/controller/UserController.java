package com.jnu.ticketapi.controller;


import com.jnu.ticketapi.application.port.UserUseCase;
import com.jnu.ticketdomain.domain.dto.LoginUserRequestDto;
import com.jnu.ticketdomain.domain.dto.LoginUserResponseDto;
import lombok.RequiredArgsConstructor;
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
    public LoginUserResponseDto logInUser(@RequestBody LoginUserRequestDto loginUserRequestDto) {
        return userUseCase.login(loginUserRequestDto);
    }
}
