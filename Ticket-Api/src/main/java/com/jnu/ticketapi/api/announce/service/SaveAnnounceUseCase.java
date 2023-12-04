package com.jnu.ticketapi.api.announce.service;


import com.jnu.ticketapi.api.announce.model.request.SaveAnnounceRequest;
import com.jnu.ticketapi.api.announce.model.response.SaveAnnounceResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.announce.adaptor.AnnounceAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SaveAnnounceUseCase {

    private final AnnounceAdaptor announceAdaptor;

    @Transactional
    public SaveAnnounceResponse execute(SaveAnnounceRequest saveAnnounceRequest){
        return SaveAnnounceResponse.of(announceAdaptor.save(saveAnnounceRequest.toEntity()));
    }
}
