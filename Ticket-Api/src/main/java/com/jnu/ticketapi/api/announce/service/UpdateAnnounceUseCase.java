package com.jnu.ticketapi.api.announce.service;

import com.jnu.ticketapi.api.announce.model.request.UpdateAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.response.UpdateAnnounceResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.announce.adaptor.AnnounceAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateAnnounceUseCase {

    private final AnnounceAdaptor announceAdaptor;

    @Transactional
    public UpdateAnnounceResponse execute(Long announceId, UpdateAnnounceRequest updateAnnounceRequest){
        return UpdateAnnounceResponse.of(announceAdaptor.update(announceId, updateAnnounceRequest.announceTitle(), updateAnnounceRequest.announceContent()));
    }
}
