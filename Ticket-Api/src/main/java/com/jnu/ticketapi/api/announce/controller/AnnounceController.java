package com.jnu.ticketapi.api.announce.controller;


import com.jnu.ticketapi.api.announce.docs.DeleteAnnounceExceptionDocs;
import com.jnu.ticketapi.api.announce.docs.GetAnnounceExceptionDocs;
import com.jnu.ticketapi.api.announce.docs.UpdateAnnounceExceptionDocs;
import com.jnu.ticketapi.api.announce.model.request.SaveAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.request.UpdateAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.response.*;
import com.jnu.ticketapi.api.announce.service.DeleteAnnounceUseCase;
import com.jnu.ticketapi.api.announce.service.GetAnnouncesUseCase;
import com.jnu.ticketapi.api.announce.service.SaveAnnounceUseCase;
import com.jnu.ticketapi.api.announce.service.UpdateAnnounceUseCase;
import com.jnu.ticketapi.api.coupon.docs.CreateCouponExceptionDocs;
import com.jnu.ticketcommon.annotation.ApiErrorExceptionsExample;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/v1")
@Tag(name = "4. [공지사항]")
@RequiredArgsConstructor
public class AnnounceController {

    private final GetAnnouncesUseCase getAnnouncesUseCase;
    private final SaveAnnounceUseCase saveAnnounceUseCase;
    private final UpdateAnnounceUseCase updateAnnounceUseCase;
    private final DeleteAnnounceUseCase deleteAnnounceUseCase;

    @GetMapping("/announce")
    @Operation(summary = "공지사항 목록 조회", description = "페이지네이션(페이지 번호, 페이지 개수, 정렬)")
    public ResponseEntity<AnnouncePagingResponse> getAnnounces(
            @PageableDefault(
                            sort = {"createdAt"},
                            direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ResponseEntity.ok(getAnnouncesUseCase.execute(pageable));
    }

    @PostMapping("/announce")
    @Operation(summary = "공지사항 작성", description = "공지사항 제목, 공지사항 내용")
    @SecurityRequirement(name = "access-token")
    public ResponseEntity<SaveAnnounceResponse> saveAnnounce(
            @RequestBody SaveAnnounceRequest saveAnnounceRequest) {
        return ResponseEntity.ok(saveAnnounceUseCase.execute(saveAnnounceRequest));
    }

    @PutMapping("/announce/{announceId}")
    @Operation(summary = "공지사항 수정", description = "공지사항 제목, 공지사항 내용")
    @ApiErrorExceptionsExample(UpdateAnnounceExceptionDocs.class)
    @SecurityRequirement(name = "access-token")
    public ResponseEntity<UpdateAnnounceResponse> updateAnnounce(
            @PathVariable Long announceId,
            @RequestBody UpdateAnnounceRequest updateAnnounceRequest) {
        return ResponseEntity.ok(updateAnnounceUseCase.execute(announceId, updateAnnounceRequest));
    }

    @DeleteMapping("/announce/{announceId}")
    @Operation(summary = "공지사항 삭제", description = "공지사항 ID")
    @ApiErrorExceptionsExample(DeleteAnnounceExceptionDocs.class)
    @SecurityRequirement(name = "access-token")
    public ResponseEntity<DeleteAnnounceResponse> deleteAnnounce(@PathVariable Long announceId) {

        return ResponseEntity.ok(deleteAnnounceUseCase.execute(announceId));
    }

    @Operation(summary = "공지사항 최신 게시글 하나 조회(메인화면)")
    @GetMapping("/announce/last")
    @ApiErrorExceptionsExample(GetAnnounceExceptionDocs.class)
    public ResponseEntity<AnnounceResponse> getAnnounce() {
        return ResponseEntity.ok(getAnnouncesUseCase.getOne());
    }

    @Operation(summary = "공지사항 상세조회")
    @GetMapping("/announce/{announceId}")
    @ApiErrorExceptionsExample(GetAnnounceExceptionDocs.class)
    public ResponseEntity<AnnounceDetailsResponse> getAnnounceDetails(
            @PathVariable Long announceId) {
        return ResponseEntity.ok(getAnnouncesUseCase.getOneDetails(announceId));
    }
}
