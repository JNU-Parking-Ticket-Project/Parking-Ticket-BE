package com.jnu.ticketapi.api.announce.model.request;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.Builder;

public record SaveAnnounceRequest(String announceTitle, String announceContent) {
    @Builder
    public SaveAnnounceRequest {}

    public Announce toEntity() {
        return Announce.builder()
                .announceTitle(this.announceTitle)
                .announceContent(this.announceContent)
                .build();
    }
}
