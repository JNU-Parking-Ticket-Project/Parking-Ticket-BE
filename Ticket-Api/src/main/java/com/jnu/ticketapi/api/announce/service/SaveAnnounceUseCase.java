package com.jnu.ticketapi.api.announce.service;


import com.jnu.ticketapi.api.announce.model.request.SaveAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.response.SaveAnnounceResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.announce.adaptor.AnnounceAdaptor;
import com.jnu.ticketdomain.domains.announce.adaptor.AnnounceImageAdaptor;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SaveAnnounceUseCase {

    private final AnnounceAdaptor announceAdaptor;
    private final AnnounceImageAdaptor announceImageAdaptor;

    @Transactional
    public SaveAnnounceResponse execute(SaveAnnounceRequest saveAnnounceRequest) {
        Announce announce = announceAdaptor.save(saveAnnounceRequest.toAnnounce());
        return SaveAnnounceResponse.from(
                announce,
                announceImageAdaptor.saveAll(saveAnnounceRequest.toAnnounceImages(announce)));
    }
}
