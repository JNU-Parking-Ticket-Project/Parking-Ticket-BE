package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.dto.SuccessResponse;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.common.aop.event.EventTypeCheck;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.AlreadyExistPublishEventException;
import com.jnu.ticketdomain.domains.events.exception.AlreadyUnpublishEventException;
import com.jnu.ticketdomain.domains.events.exception.CannotPublishClosedEventException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdatePublishStatusUseCase {

    private final EventAdaptor eventAdaptor;

    @Transactional
    public SuccessResponse execute(Long eventId, boolean publish) {
        Event event = eventAdaptor.findById(eventId);
        if(event.getEventStatus().equals(EventStatus.CLOSED)) {
            throw CannotPublishClosedEventException.EXCEPTION;
        }
        if (publish) {
            checkExistPublishTrue();
            checkPublishStatus(event, true);
            event.isPublish(true);
            return new SuccessResponse(ResponseMessage.PUBLISH_SUCCESS_TRUE_MESSAGE);
        } else {
            checkPublishStatus(event, false);
            event.isPublish(false);
            return new SuccessResponse(ResponseMessage.UNPUBLISH_SUCCESS_TRUE_MESSAGE);
        }
    }

    private void checkExistPublishTrue() {
        if(eventAdaptor.existsByPublishTrue()) {
            throw AlreadyExistPublishEventException.EXCEPTION;
        }
    }

    private void checkPublishStatus(Event event, boolean publish) {
        if(Boolean.TRUE.equals(event.getPublish()) == publish) {
            if(publish)
                throw AlreadyExistPublishEventException.EXCEPTION;
            else
                throw AlreadyUnpublishEventException.EXCEPTION;
        }
    }
}
