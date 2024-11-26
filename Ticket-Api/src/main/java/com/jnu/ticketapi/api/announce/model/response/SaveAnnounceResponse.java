package com.jnu.ticketapi.api.announce.model.response;


import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

public record SaveAnnounceResponse(
        String announceTitle, String announceContent, List<String> announceUrl) {
    @Builder
    public SaveAnnounceResponse {}

    public static SaveAnnounceResponse from(Announce announce, List<AnnounceImage> announceImage) {
        return SaveAnnounceResponse.builder()
                .announceTitle(announce.getAnnounceTitle())
                .announceContent(announce.getAnnounceContent())
                .announceUrl(
                        announceImage.stream()
                                .map(AnnounceImage::getImageUrl)
                                .collect(Collectors.toList()))
                .build();
    }
}
