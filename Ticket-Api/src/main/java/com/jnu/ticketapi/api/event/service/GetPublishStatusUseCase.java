package com.jnu.ticketapi.api.event.service;

import com.jnu.ticketapi.api.event.model.response.PublishStatusResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetPublishStatusUseCase {

    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public PublishStatusResponse execute(Long eventId){
        return PublishStatusResponse.of(eventAdaptor.findById(eventId).getPublish());
    }

}
