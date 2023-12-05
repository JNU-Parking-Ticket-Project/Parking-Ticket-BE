package com.jnu.ticketapi.api.announce.model.request;


import lombok.Builder;

public record UpdateAnnounceRequest(String announceTitle, String announceContent) {
    @Builder
    public UpdateAnnounceRequest {}
}
