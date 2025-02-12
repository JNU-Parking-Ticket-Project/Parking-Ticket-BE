package com.jnu.ticketapi.api.announce.model.request;


import com.jnu.ticketcommon.annotation.AnnounceContent;
import com.jnu.ticketcommon.annotation.AnnounceTitle;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import com.jnu.ticketdomain.domains.announce.message.AnnounceValidationMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

public record SaveAnnounceRequest(
        @AnnounceTitle(message = AnnounceValidationMessage.INVALID_TITLE_LENGTH)
                String announceTitle,
        @AnnounceContent(message = AnnounceValidationMessage.INVALID_CONTENT_LENGTH)
                String announceContent,
        List<String> imageUrls) {
    @Builder
    public SaveAnnounceRequest {}

    public Announce toAnnounce() {
        return Announce.builder()
                .announceTitle(this.announceTitle)
                .announceContent(this.announceContent)
                .build();
    }

    public List<AnnounceImage> toAnnounceImages(Announce announce) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .map(url -> AnnounceImage.builder().imageUrl(url).announce(announce).build())
                .collect(Collectors.toList());
    }
}
