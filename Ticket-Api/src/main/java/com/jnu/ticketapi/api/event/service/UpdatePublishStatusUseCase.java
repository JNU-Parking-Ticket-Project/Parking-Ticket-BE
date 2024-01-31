package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdatePublishStatusUseCase {

    private final EventAdaptor eventAdaptor;

    @Transactional
    public void execute(Long eventId) {
        eventAdaptor.findById(eventId).updatePublishStatus(true);
    }
}
