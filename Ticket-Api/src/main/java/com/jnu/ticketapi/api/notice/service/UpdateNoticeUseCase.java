package com.jnu.ticketapi.api.notice.service;


import com.jnu.ticketapi.api.notice.model.request.UpdateNoticeRequest;
import com.jnu.ticketapi.api.notice.model.response.UpdateNoticeResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.notice.adaptor.NoticeAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateNoticeUseCase {

    private final NoticeAdaptor noticeAdaptor;

    @Transactional
    public UpdateNoticeResponse updateNotice(UpdateNoticeRequest updateNoticeRequest) {
        return UpdateNoticeResponse.from(
                noticeAdaptor.updateNotice(updateNoticeRequest.noticeContent()));
    }
}
