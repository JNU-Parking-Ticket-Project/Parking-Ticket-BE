package com.jnu.ticketdomain.domains.notice.adaptor;

import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketcommon.exception.NoticeNotExistException;
import com.jnu.ticketdomain.domains.notice.domain.Notice;
import com.jnu.ticketdomain.domains.notice.out.NoticeLoadPort;
import com.jnu.ticketdomain.domains.notice.out.NoticeRecordPort;
import com.jnu.ticketdomain.domains.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * 안내사항 Adaptor
 *
 * @author cookie
 * @version 1.0
 */
@Adaptor
@RequiredArgsConstructor
public class NoticeAdaptor implements NoticeRecordPort, NoticeLoadPort {

    private final NoticeRepository noticeRepository;

    /**
     * 안내사항을 조회한다.
     * 안내사항이 존재하지 않는 경우 404 Error를 반환한다.
     *
     * @return Notice
     */
    @Override
    public Notice findLastOne() {
        return noticeRepository.findFirstByOrderByNoticeIdDesc()
                .orElseThrow(() -> NoticeNotExistException.EXCEPTION);
    }

    /**
     * 안내사항을 업데이트한다.
     * 안내사항이 존재하지 않는 경우 작성된 안내사항을 저장 후 해당 안내사항을 반환한다.
     *
     * @param noticeContent : 수정할 안내사항 내용
     * @return Notice
     */
    @Override
    public Notice updateNotice(String noticeContent) {
        Optional<Notice> notice = noticeRepository.findFirstByOrderByNoticeIdDesc();
        if(notice.isPresent()){
            notice.get().updateContent(noticeContent);
            return notice.get();
        }else{
            return noticeRepository.save(Notice.builder()
                    .noticeContent(noticeContent)
                    .build());
        }
    }
}
