package com.jnu.ticketapi.api.notice.model.response;


import com.jnu.ticketdomain.domains.notice.domain.Notice;
import lombok.Builder;

public record NoticeResponse(String noticeContent) {
    @Builder
    public NoticeResponse {}

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder().noticeContent(notice.getNoticeContent()).build();
    }
}
