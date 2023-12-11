package com.jnu.ticketapi.api.announce.model.response;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.Builder;

public record SaveAnnounceResponse(String announceTitle, String announceContent) {
    @Builder
    public SaveAnnounceResponse {}

    public static SaveAnnounceResponse from(Announce announce) {
        return SaveAnnounceResponse.builder()
                .announceTitle(announce.getAnnounceTitle())
                .announceContent(announce.getAnnounceContent())
                .build();
    }
}
