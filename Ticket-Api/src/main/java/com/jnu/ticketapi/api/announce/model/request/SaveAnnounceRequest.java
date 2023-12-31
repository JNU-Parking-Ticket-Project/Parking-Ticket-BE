package com.jnu.ticketapi.api.announce.model.request;


import com.jnu.ticketcommon.annotation.AnnounceContent;
import com.jnu.ticketcommon.annotation.AnnounceTitle;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.message.AnnounceValidationMessage;
import lombok.Builder;

public record SaveAnnounceRequest(
        @AnnounceTitle(message = AnnounceValidationMessage.INVALID_TITLE_LENGTH)
                String announceTitle,
        @AnnounceContent(message = AnnounceValidationMessage.INVALID_CONTENT_LENGTH)
                String announceContent) {
    @Builder
    public SaveAnnounceRequest {}

    public Announce toEntity() {
        return Announce.builder()
                .announceTitle(this.announceTitle)
                .announceContent(this.announceContent)
                .build();
    }
}
