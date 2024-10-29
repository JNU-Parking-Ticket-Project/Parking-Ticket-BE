package com.jnu.ticketapi.api.announce.service;


import com.jnu.ticketapi.api.announce.model.request.UpdateAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.response.UpdateAnnounceResponse;
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
public class UpdateAnnounceUseCase {

    private final AnnounceAdaptor announceAdaptor;
    private final AnnounceImageAdaptor announceImageAdaptor;

    @Transactional
    public UpdateAnnounceResponse execute(
            Long announceId, UpdateAnnounceRequest updateAnnounceRequest) {
        Announce announce =
                announceAdaptor.update(
                        announceId,
                        updateAnnounceRequest.announceTitle(),
                        updateAnnounceRequest.announceContent());
        return UpdateAnnounceResponse.from(
                announce, announceImageAdaptor.updateAll(updateAnnounceRequest.from(announce)));
    }
}
