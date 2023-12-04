package com.jnu.ticketapi.api.announce.model.response;

import lombok.Builder;

public record DeleteAnnounceResponse(
        String message
) {
    private final static String DELETE_MESSAGE = "공지사항이 삭제 되었습니다.";
    @Builder
    public DeleteAnnounceResponse{}

    public static DeleteAnnounceResponse of(){
        return DeleteAnnounceResponse.builder()
                .message(DELETE_MESSAGE)
                .build();
    }
}
