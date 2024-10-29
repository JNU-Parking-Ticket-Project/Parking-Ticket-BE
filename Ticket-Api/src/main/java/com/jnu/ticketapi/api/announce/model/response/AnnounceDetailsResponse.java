package com.jnu.ticketapi.api.announce.model.response;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

public record AnnounceDetailsResponse(
        Long announceId,
        String announceTitle,
        String announceContent,
        LocalDateTime announceCreatedAt,
        List<String> imageUrls) {
    @Builder
    public AnnounceDetailsResponse {}

    public static AnnounceDetailsResponse from(
            Announce announce, List<AnnounceImage> announceImages) {
        return AnnounceDetailsResponse.builder()
                .announceId(announce.getAnnounceId())
                .announceTitle(announce.getAnnounceTitle())
                .announceContent(announce.getAnnounceContent())
                .announceCreatedAt(announce.getCreatedAt())
                .imageUrls(
                        announceImages.stream()
                                .map(AnnounceImage::getImageUrl)
                                .collect(Collectors.toList()))
                .build();
    }
}
