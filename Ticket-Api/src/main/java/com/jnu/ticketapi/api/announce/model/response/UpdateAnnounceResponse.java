package com.jnu.ticketapi.api.announce.model.response;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.Builder;

public record UpdateAnnounceResponse(String announceTitle, String announceContent) {
    @Builder
    public UpdateAnnounceResponse {}

    public static UpdateAnnounceResponse from(Announce announce) {
        return UpdateAnnounceResponse.builder()
                .announceTitle(announce.getAnnounceTitle())
                .announceContent(announce.getAnnounceContent())
                .build();
    }
}
