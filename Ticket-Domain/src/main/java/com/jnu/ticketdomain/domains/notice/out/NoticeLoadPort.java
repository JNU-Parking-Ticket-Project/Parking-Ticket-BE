package com.jnu.ticketdomain.domains.notice.out;


import com.jnu.ticketdomain.domains.notice.domain.Notice;

public interface NoticeLoadPort {

    Notice findLastOne();
}
