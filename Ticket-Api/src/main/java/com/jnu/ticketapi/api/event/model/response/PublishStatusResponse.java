package com.jnu.ticketapi.api.event.model.response;

import lombok.Builder;

public record PublishStatusResponse(
        Boolean publish
) {
    @Builder
    public PublishStatusResponse{}

    public static PublishStatusResponse of(Boolean publish){
        return PublishStatusResponse.builder()
                .publish(publish)
                .build();
    }
}
