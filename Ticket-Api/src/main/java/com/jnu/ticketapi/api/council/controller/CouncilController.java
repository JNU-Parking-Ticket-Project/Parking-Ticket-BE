package com.jnu.ticketapi.api.council.controller;


import com.jnu.ticketapi.api.council.docs.CouncilSendEmailException;
import com.jnu.ticketapi.api.council.docs.CouncilSignUpExceptionDocs;
import com.jnu.ticketapi.api.council.docs.EmailOutboxQueryExceptionDocs;
import com.jnu.ticketapi.api.council.docs.EmailOutboxRecoveryExceptionDocs;
import com.jnu.ticketapi.api.council.model.request.SignUpCouncilRequest;
import com.jnu.ticketapi.api.council.model.response.FailedEmailOutboxesResponse;
import com.jnu.ticketapi.api.council.model.response.RequeueEmailOutboxResponse;
import com.jnu.ticketapi.api.council.model.response.SendEmailManuallyResponse;
import com.jnu.ticketapi.api.council.model.response.SignUpCouncilResponse;
import com.jnu.ticketapi.api.council.service.CouncilUseCase;
import com.jnu.ticketapi.api.council.service.EmailOutboxRecoveryUseCase;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "6. [학생회]")
public class CouncilController {
    private final CouncilUseCase councilUseCase;
    private final EmailOutboxRecoveryUseCase emailOutboxRecoveryUseCase;

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

    @Operation(summary = "최종 실패 메일 조회", description = "이벤트별 최종 실패 메일 Outbox를 조회")
    @GetMapping("/council/events/{eventId}/email-outboxes/failed")
    @ApiErrorExceptionsExample(EmailOutboxQueryExceptionDocs.class)
    public ResponseEntity<FailedEmailOutboxesResponse> getFailedEmailOutboxes(
            @PathVariable Long eventId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(emailOutboxRecoveryUseCase.findFailed(eventId, pageable));
    }

    @Operation(summary = "최종 실패 메일 재처리", description = "최종 실패 Outbox를 기존 발송 worker의 대기 상태로 복원")
    @PostMapping("/council/events/{eventId}/email-outboxes/{outboxId}/requeue")
    @ApiErrorExceptionsExample(EmailOutboxRecoveryExceptionDocs.class)
    public ResponseEntity<RequeueEmailOutboxResponse> requeueFailedEmailOutbox(
            @PathVariable Long eventId, @PathVariable Long outboxId) {
        return ResponseEntity.ok(emailOutboxRecoveryUseCase.requeue(eventId, outboxId));
    }
}
