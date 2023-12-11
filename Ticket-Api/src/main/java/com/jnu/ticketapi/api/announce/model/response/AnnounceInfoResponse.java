package com.jnu.ticketapi.api.announce.model.response;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;

public record AnnounceInfoResponse(
        Long announceId, String announceTitle, LocalDateTime announceCreatedAt) {

    @Builder(access = AccessLevel.PACKAGE)
    public AnnounceInfoResponse {}

    public static AnnounceInfoResponse from(@NotNull Announce announce) {
        return AnnounceInfoResponse.builder()
                .announceId(announce.getAnnounceId())
                .announceTitle(announce.getAnnounceTitle())
                .announceCreatedAt(announce.getCreatedAt())
                .build();
    }
}
