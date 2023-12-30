package com.jnu.ticketapi.api.notice.model.request;


import com.jnu.ticketcommon.annotation.NoticeContent;
import com.jnu.ticketdomain.domains.notice.message.NoticeValidationMessage;
import lombok.Builder;

public record UpdateNoticeRequest(
        @NoticeContent(message = NoticeValidationMessage.INVALID_CONTENT_LENGTH)
                String noticeContent) {
    @Builder
    public UpdateNoticeRequest {}
}
