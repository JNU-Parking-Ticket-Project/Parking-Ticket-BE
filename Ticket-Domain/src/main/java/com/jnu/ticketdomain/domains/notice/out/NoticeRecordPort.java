package com.jnu.ticketdomain.domains.notice.out;

import com.jnu.ticketdomain.domains.notice.domain.Notice;

public interface NoticeRecordPort {

    Notice updateNotice(String noticeContent);
}
