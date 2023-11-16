package com.jnu.ticketapi.controller;


import com.jnu.ticketapi.application.port.UserUseCase;
import com.jnu.ticketapi.common.utils.ApiResponse;
import com.jnu.ticketdomain.domains.dto.LoginUserResponseDto;
import com.jnu.ticketdomain.domains.dto.LoginUserRequestsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserUseCase userUseCase;

    @PostMapping("/login")
    public ApiResponse.ApiResult<LoginUserResponseDto> logInUser(
            @RequestBody LoginUserRequestsDto loginUserRequestsDto) {
        return ApiResponse.success(userUseCase.login(loginUserRequestsDto));
    }
}
