package com.jnu.ticketapi.api.announce.model.request;


import com.jnu.ticketcommon.annotation.AnnounceContent;
import com.jnu.ticketcommon.annotation.AnnounceTitle;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import com.jnu.ticketdomain.domains.announce.message.AnnounceValidationMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

public record UpdateAnnounceRequest(
        @AnnounceTitle(message = AnnounceValidationMessage.INVALID_TITLE_LENGTH)
                String announceTitle,
        @AnnounceContent(message = AnnounceValidationMessage.INVALID_CONTENT_LENGTH)
                String announceContent,
        List<String> imageUrls) {
    @Builder
    public UpdateAnnounceRequest {}

    public List<AnnounceImage> from(Announce announce) {
        return this.imageUrls.stream()
                .map(
                        imageUrl ->
                                AnnounceImage.builder()
                                        .imageUrl(imageUrl)
                                        .announce(announce)
                                        .build())
                .collect(Collectors.toList());
    }
}
