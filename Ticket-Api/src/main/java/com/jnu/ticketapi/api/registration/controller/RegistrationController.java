package com.jnu.ticketapi.api.registration.controller;


import com.jnu.ticketapi.api.registration.docs.FinalSaveExceptionDocs;
import com.jnu.ticketapi.api.registration.docs.TemporarySaveExceptionFDocs;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.request.TemporarySaveRequest;
import com.jnu.ticketapi.api.registration.model.response.FinalSaveResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationResponse;
import com.jnu.ticketapi.api.registration.model.response.GetRegistrationsResponse;
import com.jnu.ticketapi.api.registration.model.response.TemporarySaveResponse;
import com.jnu.ticketapi.api.registration.service.RegistrationUseCase;
import com.jnu.ticketapi.common.aop.GetEmail;
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
@Tag(name = "4. [신청]")
public class RegistrationController {

    private final RegistrationUseCase registrationUseCase;

    @Operation(
            summary = "임시 저장 조회",
            description = "임시 저장 했던 정보를 조회(임시 저장을 하지 않은 유저는 Email, Sector 빼고 null 반환)")
    @GetMapping("/registration/{event-id}")
    public ResponseEntity<GetRegistrationResponse> getRegistration(@GetEmail String email, @PathVariable("event-id") Long eventId) {
        GetRegistrationResponse responseDto = registrationUseCase.getRegistration(email, eventId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "주차권 임시 저장", description = "주차권 임시 저장(주차권 신청시 잔고 감소)")
    @PostMapping("/registration/temporary/{event-id}")
    @ApiErrorExceptionsExample(TemporarySaveExceptionFDocs.class)
    public ResponseEntity<TemporarySaveResponse> temporarySave(
            @RequestBody @Valid TemporarySaveRequest requestDto, @GetEmail String email, @PathVariable("event-id") Long eventId) {
        TemporarySaveResponse responseDto = registrationUseCase.temporarySave(requestDto, email, eventId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "1차 신청", description = "1차 신청")
    @PostMapping("/registration/{event-id}")
    @ApiErrorExceptionsExample(FinalSaveExceptionDocs.class)
    public ResponseEntity<FinalSaveResponse> finalSave(
            @RequestBody @Valid FinalSaveRequest requestDto, @GetEmail String email, @PathVariable("event-id") Long eventId) {
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
}
