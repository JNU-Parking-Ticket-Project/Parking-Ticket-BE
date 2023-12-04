package com.jnu.ticketapi.api.announce.controller;

import com.jnu.ticketapi.api.announce.model.request.SaveAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.request.UpdateAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.response.AnnouncePagingResponse;
import com.jnu.ticketapi.api.announce.model.response.SaveAnnounceResponse;
import com.jnu.ticketapi.api.announce.model.response.UpdateAnnounceResponse;
import com.jnu.ticketapi.api.announce.service.GetAnnouncesUseCase;
import com.jnu.ticketapi.api.announce.service.SaveAnnounceUseCase;
import com.jnu.ticketapi.api.announce.service.UpdateAnnounceUseCase;
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

    @GetMapping("/announce")
    @Operation(summary = "공지사항 목록 조회", description = "페이지네이션(페이지 번호, 페이지 개수, 정렬)")
    public ResponseEntity<AnnouncePagingResponse> getAnnounces(@PageableDefault(sort ={"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable){
        return ResponseEntity.ok(getAnnouncesUseCase.execute(pageable));
    }

    @PostMapping("/announce")
    @Operation(summary = "공지사항 작성", description = "공지사항 제목, 공지사항 내용")
    @SecurityRequirement(name = "access-token")
    public ResponseEntity<SaveAnnounceResponse> saveAnnounce(@RequestBody SaveAnnounceRequest saveAnnounceRequest){
        return ResponseEntity.ok(saveAnnounceUseCase.execute(saveAnnounceRequest));
    }

    @PutMapping("/announce/{announceId}")
    @Operation(summary = "공지사항 수정", description = "공지사항 제목, 공지사항 내용")
    @SecurityRequirement(name = "access-token")
    public ResponseEntity<UpdateAnnounceResponse> updateAnnounce(@PathVariable Long announceId,
                                                                 @RequestBody UpdateAnnounceRequest updateAnnounceRequest){
        return ResponseEntity.ok(updateAnnounceUseCase.execute(announceId, updateAnnounceRequest));
    }

}
