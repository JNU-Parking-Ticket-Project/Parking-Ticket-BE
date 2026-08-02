package com.jnu.ticketapi.api.registration.controller;


import com.jnu.ticketapi.api.registration.docs.FinalSaveExceptionDocs;
import com.jnu.ticketapi.api.registration.docs.TemporarySaveExceptionFDocs;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.*;
import com.jnu.ticketapi.api.registration.service.RegistrationUseCase;
import com.jnu.ticketapi.common.aop.GetEmail;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "4. [신청]")
public class RegistrationController {

    private final RegistrationUseCase registrationUseCase;

    @Operation(
            summary = "임시 저장 조회",
            description = "임시 저장 했던 정보를 조회(임시 저장을 하지 않은 유저는 Email, Sector 빼고 null 반환)")
    @GetMapping("/registration/{event-id}")
    public ResponseEntity<GetRegistrationResponse> getRegistration(
            @GetEmail String email, @PathVariable("event-id") Long eventId) {
        GetRegistrationResponse responseDto = registrationUseCase.getRegistration(email, eventId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "주차권 임시 저장", description = "주차권 임시 저장(주차권 신청시 잔고 감소)")
    @PostMapping("/registration/temporary/{event-id}")
    @ApiErrorExceptionsExample(TemporarySaveExceptionFDocs.class)
    public ResponseEntity<TemporarySaveResponse> temporarySave(
            @RequestBody @Valid TemporarySaveRequest requestDto,
            @Parameter(hidden = true) @GetEmail String email,
            @PathVariable("event-id") Long eventId) {
        TemporarySaveResponse responseDto =
                registrationUseCase.temporarySave(requestDto, email, eventId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "1차 신청",
            description =
                    "Redis에서 접수 순서와 결과를 확정하고 최소 결정 저널을 기록한 뒤 응답하며 신청서와 Email Outbox 저장은 비동기로 처리")
    @PostMapping("/registration/{event-id}")
    @ApiErrorExceptionsExample(FinalSaveExceptionDocs.class)
    public ResponseEntity<FinalSaveResponse> finalSave(
            @RequestBody @Valid FinalSaveRequest requestDto,
            @Parameter(hidden = true) @GetEmail String email,
            @PathVariable("event-id") Long eventId) {
        FinalSaveResponse responseDto = registrationUseCase.finalSave(requestDto, email, eventId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "신청 목록 조회", description = "신청 목록 조회")
    @GetMapping("/registrations/{eventId}")
    public ResponseEntity<GetRegistrationsResponse> getRegistrations(
            @PathVariable("eventId") Long eventId) {
        GetRegistrationsResponse responseDto = registrationUseCase.getRegistrations(eventId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "신청 결과 확정 확인",
            description = "신청 결과는 접수 결정 저널 저장 시점에 확정되며 기존 관리자 화면과의 호환을 위해 유지")
    @PostMapping("/registrations/assign/result/{eventId}")
    public ResponseEntity<AssignResultResponse> assignResult(
            @PathVariable("eventId") Long eventId) {
        return ResponseEntity.ok().body(new AssignResultResponse("신청 결과는 접수 결정 저널 저장 시점에 확정됩니다."));
    }
}
