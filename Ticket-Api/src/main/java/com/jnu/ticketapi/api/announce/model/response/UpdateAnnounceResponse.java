package com.jnu.ticketapi.api.announce.model.response;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

public record UpdateAnnounceResponse(
        String announceTitle, String announceContent, List<String> imageUrl) {
    @Builder
    public UpdateAnnounceResponse {}

    public static UpdateAnnounceResponse from(
            Announce announce, List<AnnounceImage> announceImages) {
        return UpdateAnnounceResponse.builder()
                .announceTitle(announce.getAnnounceTitle())
                .announceContent(announce.getAnnounceContent())
                .imageUrl(
                        announceImages.stream()
                                .map(AnnounceImage::getImageUrl)
                                .collect(Collectors.toList()))
                .build();
    }
}
