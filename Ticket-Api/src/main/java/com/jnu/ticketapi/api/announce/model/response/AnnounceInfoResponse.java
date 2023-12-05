package com.jnu.ticketapi.api.announce.model.response;

import com.jnu.ticketdomain.domains.announce.domain.Announce;
import lombok.AccessLevel;
import lombok.Builder;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AnnounceInfoResponse(
        Long announceId,
        String announceTitle,
        LocalDateTime announceCreatedAt
) {

    @Builder(access = AccessLevel.PACKAGE)
    public AnnounceInfoResponse {}
    public static AnnounceInfoResponse of(@NotNull Announce announce) {
        return AnnounceInfoResponse.builder()
                .announceId(announce.getAnnounceId())
                .announceTitle(announce.getAnnounceTitle())
                .announceCreatedAt(announce.getCreatedAt())
                .build();

    }
}
