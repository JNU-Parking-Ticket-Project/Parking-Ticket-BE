package com.jnu.ticketapi.api.notice.model.response;


import com.jnu.ticketdomain.domains.notice.domain.Notice;
import lombok.Builder;

public record UpdateNoticeResponse(String noticeContent) {
    @Builder
    public UpdateNoticeResponse {}

    public static UpdateNoticeResponse from(Notice notice) {
        return UpdateNoticeResponse.builder().noticeContent(notice.getNoticeContent()).build();
    }
}
