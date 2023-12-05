package com.jnu.ticketapi.api.announce.model.response;

import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.Builder;

import java.time.LocalDateTime;

public record AnnounceDetailsResponse(
        Long announceId,
        String announceTitle,
        String announceContent,
        LocalDateTime announceCreatedAt
) {
    @Builder
    public AnnounceDetailsResponse{}

    public static AnnounceDetailsResponse of(Announce announce){
        return AnnounceDetailsResponse.builder()
                .announceId(announce.getAnnounceId())
                .announceTitle(announce.getAnnounceTitle())
                .announceContent(announce.getAnnounceContent())
                .announceCreatedAt(announce.getCreatedAt())
                .build();
    }
}
