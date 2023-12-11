package com.jnu.ticketapi.api.captcha.controller;


import com.jnu.ticketapi.api.captcha.docs.GetCaptchaExceptionDocs;
import com.jnu.ticketapi.api.captcha.model.response.CaptchaResponse;
import com.jnu.ticketapi.api.captcha.service.GetCaptchaUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@SecurityRequirement(name = "access-token")
@Controller
@RequestMapping("/v1")
@Tag(name = "7. [캡챠]")
@RequiredArgsConstructor
public class CaptchaController {

    private final GetCaptchaUseCase getCaptchaUseCase;

    @GetMapping("/captcha")
    @Operation(
            summary = "캡챠 인증 요청",
            description = "캡챠 인증하기 위한 정보를 요청합니다. 이는 1차 신청 POST 요청으로 이어집니다.")
    @ApiErrorExceptionsExample(GetCaptchaExceptionDocs.class)
    public ResponseEntity<CaptchaResponse> getCaptcha() {
        return ResponseEntity.ok(getCaptchaUseCase.execute());
    }
}
