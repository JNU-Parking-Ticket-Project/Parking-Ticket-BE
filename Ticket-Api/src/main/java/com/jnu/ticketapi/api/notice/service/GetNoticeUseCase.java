package com.jnu.ticketapi.api.notice.service;

import com.jnu.ticketapi.api.notice.model.response.NoticeResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.notice.adaptor.NoticeAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetNoticeUseCase {

    private final NoticeAdaptor noticeAdaptor;

    @Transactional(readOnly = true)
    public NoticeResponse getNoticeDetails(){
        return NoticeResponse.of(noticeAdaptor.findLastOne());
    }
}
