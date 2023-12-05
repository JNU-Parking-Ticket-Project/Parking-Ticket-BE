package com.jnu.ticketdomain.domains.notice.repository;

import com.jnu.ticketdomain.domains.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Optional<Notice> findFirstByOrderByNoticeIdDesc();
}
