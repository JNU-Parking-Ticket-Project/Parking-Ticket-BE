package com.jnu.ticketapi.api.announce.model.response;

import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.Builder;

public record AnnounceResponse(
        Long announceId,
        String announceTitle
) {
    @Builder
    public AnnounceResponse{}

    public static AnnounceResponse of(Announce announce){
        return AnnounceResponse.builder()
                .announceId(announce.getAnnounceId())
                .announceTitle(announce.getAnnounceTitle())
                .build();
    }
}
