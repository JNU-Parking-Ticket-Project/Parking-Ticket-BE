package com.jnu.ticketapi.api.announce.service;


import com.jnu.ticketapi.api.announce.model.response.AnnounceDetailsResponse;
import com.jnu.ticketapi.api.announce.model.response.AnnouncePagingResponse;
import com.jnu.ticketapi.api.announce.model.response.AnnounceResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.announce.adaptor.AnnounceAdaptor;
import com.jnu.ticketdomain.domains.announce.adaptor.AnnounceImageAdaptor;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetAnnouncesUseCase {

    private final AnnounceAdaptor announceAdaptor;
    private final AnnounceImageAdaptor announceImageAdaptor;

    @Transactional(readOnly = true)
    public AnnouncePagingResponse execute(Pageable pageable) {
        Page<Announce> announcePage = announceAdaptor.findAllByOrderByCreatedAtDesc(pageable);
        return AnnouncePagingResponse.of(announcePage);
    }

    @Transactional(readOnly = true)
    public AnnounceResponse getOne() {
        return AnnounceResponse.from(announceAdaptor.findAnnounceByLastOne());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "announceCache", key = "#announceId", cacheManager = "ehcacheManager")
    public AnnounceDetailsResponse getOneDetails(Long announceId) {
        Announce announce = announceAdaptor.findById(announceId);
        return AnnounceDetailsResponse.from(
                announce, announceImageAdaptor.findByAnnounce(announce));
    }
}
