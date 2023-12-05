package com.jnu.ticketapi.api.announce.service;

import com.jnu.ticketapi.api.announce.model.response.AnnouncePagingResponse;
import com.jnu.ticketapi.api.announce.model.response.AnnounceResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.announce.adaptor.AnnounceAdaptor;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetAnnouncesUseCase {

    private final AnnounceAdaptor announceAdaptor;

    @Transactional(readOnly = true)
    public AnnouncePagingResponse execute(Pageable pageable) {
        Page<Announce> announcePage = announceAdaptor.findAllByOrderByCreatedAtDesc(pageable);
        return AnnouncePagingResponse.of(announcePage);
    }

    @Transactional(readOnly = true)
    public AnnounceResponse getOne(){
        return AnnounceResponse.of(announceAdaptor.findAnnounceByLastOne());
    }




}
