package com.jnu.ticketapi.api.announce.model.request;


import com.jnu.ticketcommon.annotation.AnnounceContent;
import com.jnu.ticketcommon.annotation.AnnounceTitle;
import com.jnu.ticketdomain.domains.announce.message.AnnounceValidationMessage;
import lombok.Builder;

public record UpdateAnnounceRequest(
        @AnnounceTitle(message = AnnounceValidationMessage.INVALID_TITLE_LENGTH)
                String announceTitle,
        @AnnounceContent(message = AnnounceValidationMessage.INVALID_CONTENT_LENGTH)
                String announceContent) {
    @Builder
    public UpdateAnnounceRequest {}
}
