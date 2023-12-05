package com.jnu.ticketapi.api.notice.controller;


import com.jnu.ticketapi.api.notice.model.request.UpdateNoticeRequest;
import com.jnu.ticketapi.api.notice.model.response.NoticeResponse;
import com.jnu.ticketapi.api.notice.model.response.UpdateNoticeResponse;
import com.jnu.ticketapi.api.notice.service.GetNoticeUseCase;
import com.jnu.ticketapi.api.notice.service.UpdateNoticeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/v1")
@Tag(name = "5. [안내사항]")
@RequiredArgsConstructor
public class NoticeController {

    private final GetNoticeUseCase getNoticeUseCase;
    private final UpdateNoticeUseCase updateNoticeUseCase;

    @GetMapping("/notice")
    @Operation(summary = "안내사항 조회")
    public ResponseEntity<NoticeResponse> getNoticeDetails() {
        return ResponseEntity.ok(getNoticeUseCase.getNoticeDetails());
    }

    @PutMapping("/notice")
    @Operation(summary = "안내사항 수정")
    @SecurityRequirement(name = "access-token")
    public ResponseEntity<UpdateNoticeResponse> updateNotice(
            @RequestBody UpdateNoticeRequest updateNoticeRequest) {
        return ResponseEntity.ok(updateNoticeUseCase.updateNotice(updateNoticeRequest));
    }
}
