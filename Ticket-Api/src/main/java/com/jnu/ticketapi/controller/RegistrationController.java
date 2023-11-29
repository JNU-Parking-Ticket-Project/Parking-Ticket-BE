package com.jnu.ticketapi.controller;


import com.jnu.ticketapi.application.port.RegistrationUseCase;
import com.jnu.ticketapi.application.port.UserUseCase;
import com.jnu.ticketapi.common.aop.GetEmail;
import com.jnu.ticketapi.dto.GetRegistrationResponseDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationUseCase registrationUseCase;
    private final UserUseCase userUseCase;

    @GetMapping("/registration")
    public ResponseEntity<GetRegistrationResponseDto> getRegistration(@GetEmail String email) {
        Long userId = userUseCase.findByEmail2(email).getId();
        GetRegistrationResponseDto responseDto = registrationUseCase.getRegistration(userId, email);
        return ResponseEntity.ok(responseDto);
    }
}
