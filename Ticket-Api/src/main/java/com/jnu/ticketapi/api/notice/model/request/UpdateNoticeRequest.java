package com.jnu.ticketapi.api.notice.model.request;

import lombok.Builder;

public record UpdateNoticeRequest(
        String noticeContent
) {
    @Builder
    public UpdateNoticeRequest{}
}
