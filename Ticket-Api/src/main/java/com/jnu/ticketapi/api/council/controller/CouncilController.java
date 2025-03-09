package com.jnu.ticketapi.api.council.controller;


import com.jnu.ticketapi.api.council.docs.CouncilSendEmailException;
import com.jnu.ticketapi.api.council.docs.CouncilSignUpExceptionDocs;
import com.jnu.ticketapi.api.council.model.request.SignUpCouncilRequest;
import com.jnu.ticketapi.api.council.model.response.SendEmailManuallyResponse;
import com.jnu.ticketapi.api.council.model.response.SignUpCouncilResponse;
import com.jnu.ticketapi.api.council.service.CouncilUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "6. [학생회]")
public class CouncilController {
    private final CouncilUseCase councilUseCase;

    @Operation(summary = "학생회 회원가입", description = "학생회 회원가입")
    @PostMapping("/council/signup")
    @ApiErrorExceptionsExample(CouncilSignUpExceptionDocs.class)
    public ResponseEntity<SignUpCouncilResponse> signUpCouncil(
            @RequestBody @Valid SignUpCouncilRequest signUpCouncilRequest) {
        SignUpCouncilResponse responseDto = councilUseCase.signUp(signUpCouncilRequest);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "메일 수동전송", description = "메일 수동전송")
    @PostMapping("/council/emails/{eventId}")
    @ApiErrorExceptionsExample(CouncilSendEmailException.class)
    public ResponseEntity<SendEmailManuallyResponse> sendEmailsByManually(
            @PathVariable Long eventId) {
        SendEmailManuallyResponse responseDto = councilUseCase.sendEmail(eventId);
        return ResponseEntity.ok(responseDto);
    }
}
