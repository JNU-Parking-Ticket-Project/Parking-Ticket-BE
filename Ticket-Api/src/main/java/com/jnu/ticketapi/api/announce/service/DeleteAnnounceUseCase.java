package com.jnu.ticketapi.api.announce.service;

import com.jnu.ticketapi.api.announce.model.response.DeleteAnnounceResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.announce.adaptor.AnnounceAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class DeleteAnnounceUseCase {
    private final AnnounceAdaptor announceAdaptor;

    @Transactional
    public DeleteAnnounceResponse execute(Long announceId){
        announceAdaptor.delete(announceId);
        return DeleteAnnounceResponse.of();
    }
}
