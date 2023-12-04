package com.jnu.ticketapi.api.announce.controller;

import com.jnu.ticketapi.api.announce.model.response.AnnouncePagingResponse;
import com.jnu.ticketapi.api.announce.service.GetAnnouncesUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AnnounceController {

    private final GetAnnouncesUseCase getAnnouncesUseCase;

    @SecurityRequirement(name = "access-token")
    @GetMapping("/announces")
    public ResponseEntity<AnnouncePagingResponse> getAnnounces(@PageableDefault(sort ={"createdAt"}, direction = Sort.Direction.DESC,size = 10) Pageable pageable){
        return ResponseEntity.ok(getAnnouncesUseCase.execute(pageable));
    }





}
